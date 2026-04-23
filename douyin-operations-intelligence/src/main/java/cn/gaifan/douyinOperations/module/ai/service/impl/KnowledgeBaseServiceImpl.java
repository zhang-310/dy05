package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.util.MilvusUserHint;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.PromptSanitizer;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.config.SearchMetricsCollector;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.KbFeedback;
import cn.gaifan.douyinOperations.module.ai.entity.KbImportReport;
import cn.gaifan.douyinOperations.module.ai.util.ChunkResult;
import cn.gaifan.douyinOperations.module.ai.util.DocumentTypeDetector;
import cn.gaifan.douyinOperations.module.ai.util.MixedDocumentProcessor;
import cn.gaifan.douyinOperations.module.ai.util.ScriptAwareChunker;
import cn.gaifan.douyinOperations.module.ai.util.ContentFingerprintUtil;
import cn.gaifan.douyinOperations.module.ai.util.SimHashUtil;
import cn.gaifan.douyinOperations.module.ai.vo.ChunkItemVO;
import cn.gaifan.douyinOperations.module.ai.vo.DedupPreviewVO;
import cn.gaifan.douyinOperations.module.ai.vo.KbDocumentSearchVO;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KbFeedbackRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KbImportReportRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.DocArchiveService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBoostService;
import cn.gaifan.douyinOperations.module.ai.service.QueryRewriteService;
import cn.gaifan.douyinOperations.module.ai.service.RerankerService;
import cn.gaifan.douyinOperations.module.ai.service.SearchService;
import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    /** 分块大小（字符，约 512 token 近似） */
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 50;

    /** RRF 融合 k 值（标准 60） */
    private static final int RRF_K = 60;
    /** 向量检索权重 */
    private static final double VECTOR_WEIGHT = 0.7;
    /** 关键词检索权重 */
    private static final double KEYWORD_WEIGHT = 0.3;
    /** 初筛 topK，用于重排 */
    private static final int RERANK_CANDIDATE_TOP = 20;
    /** Redis 缓存 TTL 秒 */
    private static final long CACHE_TTL_SECONDS = 3600;

    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private KbFeedbackRepository feedbackRepository;

    @Resource
    private KbImportReportRepository importReportRepository;

    @Resource
    private KnowledgeBoostService knowledgeBoostService;

    @Resource
    private VectorService vectorService;

    @Resource
    private SearchService searchService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RerankerService rerankerService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DocArchiveService docArchiveService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private QueryRewriteService queryRewriteService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SearchMetricsCollector searchMetricsCollector;

    @Resource
    private DocumentTypeDetector documentTypeDetector;

    @Resource
    private MixedDocumentProcessor mixedDocumentProcessor;

    @Value("${app.ai.kb.dedup.enabled:true}")
    private boolean dedupEnabled;
    @Value("${app.ai.kb.dedup.doc-fingerprint:true}")
    private boolean dedupDocFingerprint;
    @Value("${app.ai.kb.dedup.doc-simhash:true}")
    private boolean dedupDocSimhash;
    @Value("${app.ai.kb.dedup.simhash-min-length:200}")
    private int simhashMinLength;
    @Value("${app.ai.kb.dedup.simhash-distance:3}")
    private int simhashDistance;
    @Value("${app.ai.kb.dedup.skip-threshold:0.92}")
    private double dedupSkipThreshold;
    @Value("${app.ai.kb.dedup.downweight-threshold:0.80}")
    private double dedupDownweightThreshold;
    @Value("${app.ai.kb.dedup.downweight-factor:0.6}")
    private double dedupDownweightFactor;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AiCallLogService aiCallLogService;

    @Value("${app.ai.kb.cache-enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.ai.embedding.dimension:1024}")
    private int embeddingDimension;

    @Value("${app.ai.hybrid.fallback-to-es-only:true}")
    private boolean fallbackToEsOnly;

    @Value("${app.ai.hybrid.fallback-to-milvus-only:true}")
    private boolean fallbackToMilvusOnly;

    /** 混合检索结果语义去重：余弦相似度 >= 此阈值视为重复只保留前者，0 表示关闭 */
    @Value("${app.ai.kb.hybrid-dedup-threshold:0}")
    private double hybridDedupThreshold;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeBase createKnowledgeBase(String name, String description, Long userId) {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setUserId(userId);
        kb.setKbName(name);
        kb.setDescription(description);
        kb.setEmbeddingModel("qwen3-embedding:0.6b");
        kb.setStatus(0);

        kb = knowledgeBaseRepository.save(kb);

        try {
            // 创建向量 Collection
            String collectionName = "kb_" + kb.getId();
            vectorService.createCollection(collectionName, embeddingDimension);

            // 创建 ES 索引
            String indexName = "kb_" + kb.getId();
            searchService.createIndex(indexName);

            kb.setStatus(1);
            knowledgeBaseRepository.save(kb);

            log.info("知识库 {} 创建成功", kb.getId());
        } catch (Exception e) {
            log.error("创建知识库失败", e);
            kb.setDeleted(1);
            knowledgeBaseRepository.save(kb);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建知识库失败: " + e.getMessage());
        }

        return kb;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long kbId, Long userId) {
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));

        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除此知识库");
        }

        try {
            // 删除向量 Collection
            String collectionName = "kb_" + kbId;
            vectorService.dropCollection(collectionName);

            // 删除 ES 索引
            String indexName = "kb_" + kbId;
            searchService.deleteIndex(indexName);

            // 软删除知识库和文档
            kb.setDeleted(1);
            knowledgeBaseRepository.save(kb);

            List<AiKbDocument> docs = documentRepository.findByKbIdAndDeletedOrderByCreateTimeDesc(kbId, 0);
            docs.forEach(doc -> {
                doc.setDeleted(1);
                documentRepository.save(doc);
            });

            log.info("知识库 {} 删除成功", kbId);
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除知识库失败: " + e.getMessage());
        }
    }

    @Override
    public void assertKbOwnership(Long kbId, Long userId) {
        if (kbId == null || userId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "参数无效");
        }
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此知识库");
        }
    }

    @Override
    public List<AiKnowledgeBase> listKnowledgeBases(Long userId) {
        return knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId) {
        return uploadDocument(kbId, title, content, fileType, userId, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId, String sourceType) {
        return uploadDocument(kbId, title, content, fileType, userId, sourceType, null);
    }

    @Override
    public AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId, String sourceType, String contentType) {
        return uploadDocument(kbId, title, content, fileType, userId, sourceType, contentType, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId, String sourceType, String contentType, Map<String, String> dirMetadata) {
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));

        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此知识库");
        }

        // 文档级预检：内容指纹完全一致则直接返回已存在文档
        if (dedupEnabled && dedupDocFingerprint) {
            String fingerprint = ContentFingerprintUtil.compute(content);
            if (fingerprint != null) {
                var existing = documentRepository.findFirstByKbIdAndContentFingerprintAndDeletedOrderByIdDesc(kbId, fingerprint, 0);
                if (existing.isPresent()) {
                    log.info("文档内容已存在，跳过导入: kbId={}, docId={}", kbId, existing.get().getId());
                    return existing.get();
                }
            }
        }
        // SimHash 疑似重复仅打日志，继续走分块与 chunk 级去重
        if (dedupEnabled && dedupDocSimhash && content.length() >= simhashMinLength) {
            long sim = SimHashUtil.compute(content);
            List<Long> existingSimhashes = documentRepository.findSimhashesByKbId(kbId);
            for (Long es : existingSimhashes) {
                if (SimHashUtil.hammingDistance(sim, es) < simhashDistance) {
                    log.warn("文档 SimHash 疑似重复: kbId={}, 海明距离<{}", kbId, simhashDistance);
                    break;
                }
            }
        }

        AiKbDocument doc = new AiKbDocument();
        doc.setKbId(kbId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setFileType(fileType);
        doc.setFileSize((long) content.length());
        doc.setStatus(0);
        if (dirMetadata != null && !dirMetadata.isEmpty()) {
            doc.setMetadata(JSON.toJSONString(dirMetadata));
        }
        doc = documentRepository.save(doc);

        if (sourceType != null && !sourceType.isBlank()) {
            doc.setSourceType(sourceType);
            documentRepository.save(doc);
        }

        try {
            List<ChunkResult> chunkResults = resolveChunks(content, contentType);
            List<ChunkWithDedup> toInsert = dedupEnabled
                    ? applyChunkDedup(kbId, chunkResults, doc.getId())
                    : toChunkWithDedup(chunkResults, 1.0);

            if (toInsert.isEmpty()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "无可索引分块（内容过短或 chunk 级去重全部被跳过），请调整内容或去重配置");
            }

            doc.setChunkCount(toInsert.size());
            doc.setTokenCount(content.length() / 4);
            if (dedupDocFingerprint) doc.setContentFingerprint(ContentFingerprintUtil.compute(content));
            if (dedupDocSimhash && content.length() >= simhashMinLength) doc.setSimhash(SimHashUtil.compute(content));

            List<List<Float>> embeddings = toInsert.stream().map(c -> c.embedding).toList();
            String collectionName = "kb_" + kbId;
            List<Long> ids = new ArrayList<>();
            List<Map<String, Object>> metadata = new ArrayList<>();

            for (int i = 0; i < toInsert.size(); i++) {
                ChunkWithDedup c = toInsert.get(i);
                long chunkId = doc.getId() * 10000L + i;
                ids.add(chunkId);
                Map<String, Object> meta = new HashMap<>();
                meta.put("doc_id", doc.getId());
                meta.put("kb_id", kbId);
                meta.put("title", title);
                meta.put("text", c.cr.text());
                meta.put("chunk_index", i);
                if (!c.cr.labels().isEmpty()) meta.put("labels", new ArrayList<>(c.cr.labels()));
                if (c.boostFactor < 1.0) meta.put("boost_factor", c.boostFactor);
                metadata.add(meta);
            }

            vectorService.insertVectors(collectionName, ids, embeddings, metadata);

            String indexName = "kb_" + kbId;
            List<SearchService.DocumentWithId> esDocuments = new ArrayList<>();
            for (int i = 0; i < toInsert.size(); i++) {
                ChunkWithDedup c = toInsert.get(i);
                String esId = doc.getId() + "_" + i;
                Map<String, Object> esDoc = new HashMap<>();
                esDoc.put("doc_id", doc.getId());
                esDoc.put("kb_id", kbId);
                esDoc.put("title", title);
                esDoc.put("text", c.cr.text());
                esDoc.put("content", c.cr.text());
                esDoc.put("chunk_index", i);
                esDoc.put("created_at", new Date());
                if (!c.cr.labels().isEmpty()) esDoc.put("labels", new ArrayList<>(c.cr.labels()));
                if (c.boostFactor < 1.0) esDoc.put("boost_factor", c.boostFactor);
                esDocuments.add(new SearchService.DocumentWithId(esId, esDoc));
            }
            searchService.bulkIndexDocuments(indexName, esDocuments);

            doc.setStatus(1);
            documentRepository.save(doc);
            kb.setTotalDocuments(kb.getTotalDocuments() + 1);
            kb.setTotalTokens(kb.getTotalTokens() + doc.getTokenCount());
            knowledgeBaseRepository.save(kb);

            log.info("文档 {} 上传成功，共 {} 个分块", doc.getId(), toInsert.size());
            if (docArchiveService != null) {
                docArchiveService.archive(title, content, kb.getKbName(), null);
            }
            invalidateSearchCache(kbId);
        } catch (Exception e) {
            log.error("上传文档失败: {}", e.getMessage());
            doc.setDeleted(1);
            documentRepository.save(doc);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "上传文档失败: " + e.getMessage());
        }
        return doc;
    }

    private record ChunkWithDedup(ChunkResult cr, List<Float> embedding, double boostFactor) {}

    private List<ChunkWithDedup> toChunkWithDedup(List<ChunkResult> chunkResults, double boostFactor) {
        List<String> texts = chunkResults.stream().map(ChunkResult::text).toList();
        List<List<Float>> embeddings = vectorService.generateEmbeddings(texts);
        List<ChunkWithDedup> out = new ArrayList<>();
        for (int i = 0; i < chunkResults.size(); i++) {
            out.add(new ChunkWithDedup(chunkResults.get(i), embeddings.get(i), boostFactor));
        }
        return out;
    }

    /** Chunk 级去重：批量 embedding + batchSearch topK=1，score>=skip 跳过，>=downweight 降权 */
    private List<ChunkWithDedup> applyChunkDedup(Long kbId, List<ChunkResult> chunkResults, long docId) {
        if (chunkResults.isEmpty()) return List.of();
        String collectionName = "kb_" + kbId;
        List<String> texts = chunkResults.stream().map(ChunkResult::text).toList();
        List<List<Float>> embeddings = vectorService.generateEmbeddings(texts);
        List<List<VectorService.VectorSearchResult>> batchHits = vectorService.batchSearch(collectionName, embeddings, 1, null);
        List<ChunkWithDedup> kept = new ArrayList<>();
        for (int i = 0; i < chunkResults.size(); i++) {
            List<VectorService.VectorSearchResult> hits = (batchHits != null && i < batchHits.size()) ? batchHits.get(i) : List.of();
            float score = (hits != null && !hits.isEmpty()) ? hits.get(0).score() : 0f;
            if (score >= dedupSkipThreshold) {
                log.debug("chunk 跳过(相似度{})", score);
                continue;
            }
            double boost = score >= dedupDownweightThreshold ? dedupDownweightFactor : 1.0;
            kept.add(new ChunkWithDedup(chunkResults.get(i), embeddings.get(i), boost));
        }
        return kept;
    }

    @Override
    public DedupPreviewVO dedupPreview(Long kbId, String content, String contentType, Long userId) {
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此知识库");
        }
        List<ChunkResult> chunkResults = resolveChunks(content, contentType);
        String collectionName = "kb_" + kbId;
        List<DedupPreviewVO.DedupPreviewItem> details = new ArrayList<>();
        long skip = 0, downweight = 0, keep = 0;
        for (ChunkResult cr : chunkResults) {
            List<Float> embedding = vectorService.generateEmbedding(cr.text());
            List<VectorService.VectorSearchResult> hits = vectorService.search(collectionName, embedding, 1, null);
            float score = (hits != null && !hits.isEmpty()) ? hits.get(0).score() : 0f;
            String action = score >= dedupSkipThreshold ? "skip" : score >= dedupDownweightThreshold ? "downweight" : "keep";
            if ("skip".equals(action)) skip++;
            else if ("downweight".equals(action)) downweight++;
            else keep++;
            String nearestPreview = null;
            if (hits != null && !hits.isEmpty() && hits.get(0).metadata() != null) {
                Object text = hits.get(0).metadata().get("text");
                if (text != null) nearestPreview = truncateForPreview(String.valueOf(text), 100);
            }
            details.add(new DedupPreviewVO.DedupPreviewItem(
                    truncateForPreview(cr.text(), 100), score, nearestPreview, action));
        }
        return new DedupPreviewVO(chunkResults.size(), skip, downweight, keep, details);
    }

    private static String truncateForPreview(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }

    @Override
    public List<ChunkItemVO> getDocumentChunks(Long kbId, Long docId, Long userId) {
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此知识库");
        }
        AiKbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        if (!doc.getKbId().equals(kbId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "文档不属于该知识库");
        }
        List<ChunkResult> chunks = resolveChunks(doc.getContent(), null);
        List<ChunkItemVO> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkResult cr = chunks.get(i);
            result.add(new ChunkItemVO(cr.text(),
                    cr.labels() != null ? new ArrayList<>(cr.labels()) : List.of(), i));
        }
        return result;
    }

    /** 按 contentType（general|script|auto）解析分块；null 视为 general */
    private List<ChunkResult> resolveChunks(String content, String contentType) {
        String mode = contentType != null && !contentType.isBlank() ? contentType.trim().toLowerCase() : "general";
        if ("script".equals(mode)) {
            return ScriptAwareChunker.scriptSplit(content);
        }
        if ("auto".equals(mode)) {
            String detected = documentTypeDetector.detect(content);
            if ("script".equals(detected)) return ScriptAwareChunker.scriptSplit(content);
            if ("mixed".equals(detected)) return mixedDocumentProcessor.process(content);
        }
        return ScriptAwareChunker.generalSplit(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long docId, Long userId) {
        AiKbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));

        AiKnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKbId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));

        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除此文档");
        }

        try {
            // 删除向量
            String collectionName = "kb_" + doc.getKbId();
            List<Long> vectorIds = new ArrayList<>();
            for (int i = 0; i < doc.getChunkCount(); i++) {
                vectorIds.add(docId * 10000L + i);
            }
            vectorService.deleteVectors(collectionName, vectorIds);

            // 删除 ES 文档
            String indexName = "kb_" + doc.getKbId();
            List<String> esIds = new ArrayList<>();
            for (int i = 0; i < doc.getChunkCount(); i++) {
                esIds.add(docId + "_" + i);
            }
            searchService.bulkDeleteDocuments(indexName, esIds);

            // 软删除文档
            doc.setDeleted(1);
            documentRepository.save(doc);

            // 更新知识库统计
            kb.setTotalDocuments(kb.getTotalDocuments() - 1);
            kb.setTotalTokens(kb.getTotalTokens() - doc.getTokenCount());
            knowledgeBaseRepository.save(kb);

            log.info("文档 {} 删除成功", docId);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除文档失败: " + e.getMessage());
        }
    }

    @Override
    public List<AiKbDocument> listDocuments(Long kbId, Long userId) {
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));

        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此知识库");
        }

        return documentRepository.findByKbIdAndDeletedOrderByCreateTimeDesc(kbId, 0);
    }

    @Override
    public PageResultVO<AiKbDocument> pageDocuments(Long kbId, Long userId, KbDocumentSearchVO vo) {
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此知识库");
        }

        int page = vo.getPage() != null ? vo.getPage() : 0;
        int rows = vo.getRows() != null ? Math.min(vo.getRows(), 100) : 20;

        Specification<AiKbDocument> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("kbId"), kbId));
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + vo.getKeyword().trim().toLowerCase() + "%"));
            }
            if (vo.getSourceType() != null && !vo.getSourceType().isBlank()) {
                predicates.add(cb.equal(root.get("fileType"), vo.getSourceType()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AiKbDocument> pageResult = documentRepository.findAll(spec, PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime")));
        return PageResultVO.of(pageResult.getTotalElements(), pageResult.getContent(), page, rows);
    }

    @Override
    public List<KbImportReport> listImportReports(Long kbId, Long userId) {
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此知识库");
        }
        return importReportRepository.findByKbIdAndDeletedOrderByCreateTimeDesc(kbId, 0).stream()
                .limit(50)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitFeedback(Long docId, String query, int rating, String comment, String searchMode, Long userId) {
        query = PromptSanitizer.sanitize(query != null ? query : "");
        comment = PromptSanitizer.sanitize(comment != null ? comment : "");
        AiKbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在"));
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKbId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限反馈此文档");
        }

        KbFeedback fb = new KbFeedback();
        fb.setDocId(String.valueOf(docId));
        fb.setQuery(query != null ? query : "");
        fb.setRating(rating);
        fb.setComment(comment);
        fb.setSearchMode(searchMode);
        fb.setUserId(userId);
        feedbackRepository.save(fb);

        if (rating != 0) {
            knowledgeBoostService.updateBoostForFeedback(docId, rating);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int retrySyncDocument(AiKbDocument doc) {
        if (doc == null || doc.getContent() == null || doc.getContent().isBlank()) return 0;
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKbId()).orElse(null);
        if (kb == null) return 0;

        try {
            List<ChunkResult> chunkResults = resolveChunks(doc.getContent(), null);
            List<String> chunkTexts = chunkResults.stream().map(ChunkResult::text).toList();
            List<List<Float>> embeddings = vectorService.generateEmbeddings(chunkTexts);
            String collectionName = "kb_" + doc.getKbId();
            String indexName = collectionName;

            List<Long> ids = new ArrayList<>();
            List<Map<String, Object>> metadata = new ArrayList<>();
            List<SearchService.DocumentWithId> esDocuments = new ArrayList<>();

            for (int i = 0; i < chunkResults.size(); i++) {
                ChunkResult cr = chunkResults.get(i);
                long chunkId = doc.getId() * 10000L + i;
                ids.add(chunkId);
                Map<String, Object> meta = new HashMap<>();
                meta.put("doc_id", doc.getId());
                meta.put("kb_id", doc.getKbId());
                meta.put("title", doc.getTitle());
                meta.put("text", cr.text());
                meta.put("chunk_index", i);
                if (!cr.labels().isEmpty()) meta.put("labels", new ArrayList<>(cr.labels()));
                metadata.add(meta);

                String esId = doc.getId() + "_" + i;
                Map<String, Object> esDoc = new HashMap<>(meta);
                esDoc.put("content", cr.text());
                esDoc.put("created_at", new Date());
                esDocuments.add(new SearchService.DocumentWithId(esId, esDoc));
            }

            vectorService.deleteVectors(collectionName, ids);
            searchService.bulkDeleteDocuments(indexName, esDocuments.stream().map(d -> d.id()).toList());
            vectorService.insertVectors(collectionName, ids, embeddings, metadata);
            searchService.bulkIndexDocuments(indexName, esDocuments);

            doc.setChunkCount(chunkResults.size());
            doc.setTokenCount(doc.getContent().length() / 4);
            doc.setStatus(1);
            doc.setSyncRetryCount((doc.getSyncRetryCount() != null ? doc.getSyncRetryCount() : 0) + 1);
            doc.setLastSyncRetryAt(new java.sql.Timestamp(System.currentTimeMillis()));
            documentRepository.save(doc);

            kb.setTotalDocuments(kb.getTotalDocuments() + 1);
            kb.setTotalTokens(kb.getTotalTokens() + doc.getTokenCount());
            knowledgeBaseRepository.save(kb);

            log.info("双写补偿成功 docId={}", doc.getId());
            return 1;
        } catch (Exception e) {
            int retry = (doc.getSyncRetryCount() != null ? doc.getSyncRetryCount() : 0) + 1;
            doc.setSyncRetryCount(retry);
            doc.setLastSyncRetryAt(new java.sql.Timestamp(System.currentTimeMillis()));
            documentRepository.save(doc);
            log.warn("双写补偿失败 docId={}, retry={}: {}", doc.getId(), retry, e.getMessage());
            return 0;
        }
    }

    @Override
    public Long resolveKbIdByName(Long userId, String kbName) {
        if (userId == null || kbName == null || kbName.isBlank()) return null;
        return knowledgeBaseRepository.findByUserIdAndKbNameAndDeleted(userId, kbName.trim(), 0)
                .map(AiKnowledgeBase::getId)
                .orElse(null);
    }

    @Override
    public List<SearchResult> hybridSearch(Long kbId, String query, int topK, Long userId, String metadataFilter, boolean skipCache, boolean skipQueryRewrite) {
        long startMs = System.currentTimeMillis();
        try {
            AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));

        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此知识库");
        }

        query = PromptSanitizer.sanitizeForSearch(query);
        if (query.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "查询内容无效");

        // 尝试从 Redis 缓存读取（skipCache 时跳过，供 RAG 等用新导入文档）
            if (!skipCache && cacheEnabled && stringRedisTemplate != null) {
                String cacheKey = cacheKey(kbId, query, topK);
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    try {
                        incrementCacheStat("hit");
                        List<SearchResult> r = JSON.parseArray(cached, SearchResult.class);
                        long durationMs = System.currentTimeMillis() - startMs;
                        if (searchMetricsCollector != null) searchMetricsCollector.recordLatency(durationMs, kbId);
                        if (aiCallLogService != null) {
                            String stages = "{\"cache_lookup_ms\":" + durationMs + "}";
                            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "kb_search", null, "cache", truncateQuery(query), r.size(), null, null, durationMs, 1, null, false, null, stages));
                        }
                        return r;
                    } catch (Exception ignored) {
                    }
                }
            }

            if (cacheEnabled && stringRedisTemplate != null) incrementCacheStat("miss");
            int candidateTop = (rerankerService != null && rerankerService.isAvailable())
                    ? RERANK_CANDIDATE_TOP : topK;

            String collectionName = "kb_" + kbId;
            String indexName = "kb_" + kbId;

            // 全链路耗时采集（用于性能分析）
            long t0 = System.currentTimeMillis();
            long queryRewriteMs = 0, embeddingMs = 0, vectorSearchMs = 0, fulltextSearchMs = 0, fusionMs = 0, rerankerMs = 0, cacheWriteMs = 0;

            // 查询改写（可选：绑定抖音账号 + LLM，单次可能数秒～数十秒）
            List<String> queries;
            if (skipQueryRewrite || queryRewriteService == null) {
                queries = Collections.singletonList(query);
            } else {
                queries = queryRewriteService.rewrite(userId, query);
            }
            queryRewriteMs = System.currentTimeMillis() - t0;

            Map<String, RRFEntry> rrfMap = new ConcurrentHashMap<>();
            AtomicBoolean anyVector = new AtomicBoolean(false);
            AtomicBoolean anyFulltext = new AtomicBoolean(false);

            CircuitBreaker milvusCb = circuitBreakerRegistry != null ? circuitBreakerRegistry.circuitBreaker("milvus") : null;
            CircuitBreaker esCb = circuitBreakerRegistry != null ? circuitBreakerRegistry.circuitBreaker("elasticsearch") : null;

            AtomicLong vecEmbeddingMs = new AtomicLong();
            AtomicLong vecSearchMs = new AtomicLong();
            AtomicLong esSearchMs = new AtomicLong();

            int nq = queries.size();
            if (nq <= 1) {
                mergeHybridSubQueryIntoRrf(
                        queries.get(0), collectionName, indexName, candidateTop, metadataFilter,
                        rrfMap, anyVector, anyFulltext, milvusCb, esCb,
                        vecEmbeddingMs, vecSearchMs, esSearchMs, nq);
            } else {
                List<CompletableFuture<Void>> subFutures = new ArrayList<>();
                for (String subq : queries) {
                    subFutures.add(CompletableFuture.runAsync(() -> mergeHybridSubQueryIntoRrf(
                            subq, collectionName, indexName, candidateTop, metadataFilter,
                            rrfMap, anyVector, anyFulltext, milvusCb, esCb,
                            vecEmbeddingMs, vecSearchMs, esSearchMs, nq)));
                }
                try {
                    CompletableFuture.allOf(subFutures.toArray(CompletableFuture[]::new)).get(120, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    Throwable c = e.getCause();
                    if (c instanceof BusinessException be) {
                        throw be;
                    }
                    throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "检索失败: " + (c != null ? c.getMessage() : e.getMessage()));
                } catch (TimeoutException e) {
                    throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "检索超时（多子查询并行）");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "检索被中断");
                }
            }
            embeddingMs = vecEmbeddingMs.get();
            vectorSearchMs = vecSearchMs.get();
            fulltextSearchMs = esSearchMs.get();

            long tFusion = System.currentTimeMillis();
            List<SearchResult> merged;
            if (!anyVector.get() && !anyFulltext.get()) {
                log.warn("知识库混合检索无结果（向量与全文均无数据）。请确认：1) Elasticsearch 已启动（如 docker compose up -d elasticsearch）；2) 若使用向量检索则需启动 Milvus；3) 知识库是否已导入文档。kbId={}, query={}", kbId, query);
                merged = Collections.emptyList();
            } else {
                merged = rrfMap.values().stream()
                        .sorted((a, b) -> Double.compare(b.score, a.score))
                        .map(e -> new SearchResult(e.docId, e.title, e.text, e.score,
                                (e.sourceMask & 3) == 3 ? "hybrid" : ((e.sourceMask & 1) != 0 ? "vector" : "fulltext"),
                                Long.valueOf(e.docId * 10000L + e.chunkIndex),
                                e.labels != null ? e.labels : List.of()))
                        .limit(rerankerService != null && rerankerService.isAvailable() ? RERANK_CANDIDATE_TOP : topK)
                        .collect(Collectors.toList());

                if (hybridDedupThreshold > 0 && merged.size() > 1) {
                    merged = applySemanticDedup(merged);
                }
            }

            fusionMs = System.currentTimeMillis() - tFusion;

            // BR-31 知识权重加权：按 boost_factor 调整分数
            Set<Long> docIds = merged.stream().map(SearchResult::docId).collect(Collectors.toSet());
            if (!docIds.isEmpty()) {
                Map<Long, Double> boostMap = documentRepository.findByIdIn(List.copyOf(docIds)).stream()
                        .filter(d -> d.getBoostFactor() != null && d.getBoostFactor().doubleValue() != 1.0)
                        .collect(Collectors.toMap(AiKbDocument::getId, d -> d.getBoostFactor().doubleValue()));
                if (!boostMap.isEmpty()) {
                    merged = merged.stream()
                            .map(r -> {
                                double boost = boostMap.getOrDefault(r.docId(), 1.0);
                                return new SearchResult(r.docId(), r.title(), r.content(), r.score() * boost, r.source(), r.chunkId(), r.labels());
                            })
                            .sorted((a, b) -> Double.compare(b.score(), a.score()))
                            .collect(Collectors.toList());
                }
            }

            // 可选：重排序
            long tRerank = System.currentTimeMillis();
            if (rerankerService != null && rerankerService.isAvailable() && !merged.isEmpty()) {
                List<String> candidates = merged.stream().map(SearchResult::content).collect(Collectors.toList());
                List<Float> rerankScores = rerankerService.rerank(query, candidates);
                if (rerankScores != null && rerankScores.size() == merged.size()) {
                    List<SearchResult> withRerank = new ArrayList<>();
                    for (int i = 0; i < merged.size(); i++) {
                        SearchResult orig = merged.get(i);
                        withRerank.add(new SearchResult(
                                orig.docId(), orig.title(), orig.content(),
                                rerankScores.get(i), "reranked", orig.chunkId(), orig.labels()
                        ));
                    }
                    withRerank.sort((a, b) -> Double.compare(b.score(), a.score()));
                    merged = withRerank.stream().limit(topK).collect(Collectors.toList());
                }
            }
            rerankerMs = System.currentTimeMillis() - tRerank;

            // 写入缓存（skipCache 时跳过）
            long tCache = System.currentTimeMillis();
            if (!skipCache && cacheEnabled && stringRedisTemplate != null && !merged.isEmpty()) {
                String cacheKey = cacheKey(kbId, query, topK);
                stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(merged), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }
            cacheWriteMs = System.currentTimeMillis() - tCache;

            long durationMs = System.currentTimeMillis() - startMs;
            if (searchMetricsCollector != null) searchMetricsCollector.recordLatency(durationMs, kbId);
            if (aiCallLogService != null) {
                String stages = String.format("{\"query_rewrite_ms\":%d,\"embedding_ms\":%d,\"vector_search_ms\":%d,\"fulltext_search_ms\":%d,\"fusion_ms\":%d,\"reranker_ms\":%d,\"cache_write_ms\":%d,\"total_ms\":%d}",
                        queryRewriteMs, embeddingMs, vectorSearchMs, fulltextSearchMs, fusionMs, rerankerMs, cacheWriteMs, durationMs);
                aiCallLogService.log(new AiCallLogService.LogEntry(userId, "kb_search", null, "hybrid", truncateQuery(query), merged.size(), null, null, durationMs, 1, null, false, null, stages));
            }
            return merged;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            if (searchMetricsCollector != null) searchMetricsCollector.recordLatency(durationMs, kbId);
            if (aiCallLogService != null) {
                aiCallLogService.log(new AiCallLogService.LogEntry(userId, "kb_search", null, "hybrid", truncateQuery(query), null, null, null, durationMs, 0, e.getMessage(), false, null, null));
            }
            log.error("混合搜索失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 单个子查询：Embedding + Milvus 与 ES 并行，RRF 合并进 rrfMap（线程安全）。
     *
     * @param totalSubQueries 子查询总数；为 1 时向量/全文失败会按配置抛错；大于 1 时降级为空列表。
     */
    private void mergeHybridSubQueryIntoRrf(
            String q,
            String collectionName,
            String indexName,
            int candidateTop,
            String metadataFilter,
            Map<String, RRFEntry> rrfMap,
            AtomicBoolean anyVector,
            AtomicBoolean anyFulltext,
            CircuitBreaker milvusCb,
            CircuitBreaker esCb,
            AtomicLong vecEmbeddingMs,
            AtomicLong vecSearchMs,
            AtomicLong esSearchMs,
            int totalSubQueries
    ) {
        CompletableFuture<List<VectorService.VectorSearchResult>> vectorFuture = CompletableFuture.supplyAsync(() -> {
            long te0 = System.currentTimeMillis();
            try {
                List<Float> queryVector = vectorService.generateEmbedding(q);
                vecEmbeddingMs.addAndGet(System.currentTimeMillis() - te0);
                long ts0 = System.currentTimeMillis();
                List<VectorService.VectorSearchResult> res = (milvusCb != null
                        ? milvusCb.executeSupplier(() -> vectorService.search(collectionName, queryVector, candidateTop, metadataFilter))
                        : vectorService.search(collectionName, queryVector, candidateTop, metadataFilter));
                vecSearchMs.addAndGet(System.currentTimeMillis() - ts0);
                return res;
            } catch (CallNotPermittedException e) {
                log.warn("Milvus 熔断打开，降级为仅 ES");
                return Collections.<VectorService.VectorSearchResult>emptyList();
            } catch (Exception e) {
                if (totalSubQueries == 1 && !fallbackToEsOnly) {
                    throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL,
                            MilvusUserHint.appendRecoveryHint("向量检索失败: " + e.getMessage()));
                }
                return Collections.<VectorService.VectorSearchResult>emptyList();
            }
        });
        CompletableFuture<List<SearchService.SearchResult>> esFuture = CompletableFuture.supplyAsync(() -> {
            long ts0 = System.currentTimeMillis();
            try {
                List<SearchService.SearchResult> res = (esCb != null
                        ? esCb.executeSupplier(() -> searchService.search(indexName, q, 0, candidateTop, null))
                        : searchService.search(indexName, q, 0, candidateTop, null));
                esSearchMs.addAndGet(System.currentTimeMillis() - ts0);
                return res;
            } catch (CallNotPermittedException e) {
                log.warn("ES 熔断打开，降级为仅 Milvus");
                return Collections.<SearchService.SearchResult>emptyList();
            } catch (Exception e) {
                if (totalSubQueries == 1 && !fallbackToMilvusOnly) {
                    throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "全文检索失败: " + e.getMessage());
                }
                return Collections.<SearchService.SearchResult>emptyList();
            }
        });

        List<VectorService.VectorSearchResult> vectorResults;
        List<SearchService.SearchResult> fullTextResults;
        try {
            vectorResults = vectorFuture.get(60, TimeUnit.SECONDS);
            fullTextResults = esFuture.get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e.getCause() instanceof BusinessException be) {
                throw be;
            }
            throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "检索超时或失败: " + e.getMessage());
        }
        if (!vectorResults.isEmpty()) {
            anyVector.set(true);
        }
        if (!fullTextResults.isEmpty()) {
            anyFulltext.set(true);
        }

        for (int rank = 0; rank < vectorResults.size(); rank++) {
            VectorService.VectorSearchResult r = vectorResults.get(rank);
            Object docIdObj = r.metadata().get("doc_id");
            if (docIdObj == null) docIdObj = r.metadata().get("docId");
            if (docIdObj == null) continue;
            Long docId = ((Number) docIdObj).longValue();
            int chunkIdx = ((Number) r.metadata().getOrDefault("chunk_index", 0)).intValue();
            String key = chunkKey(docId, chunkIdx);
            String title = (String) r.metadata().get("title");
            String text = (String) r.metadata().get("text");
            List<String> labels = getLabelsFromMeta(r.metadata());

            double rrfScore = VECTOR_WEIGHT * (1.0 / (RRF_K + rank + 1));
            rrfMap.merge(key, new RRFEntry(docId, chunkIdx, title, text, rrfScore, 1, labels),
                    (a, b) -> new RRFEntry(a.docId, a.chunkIndex, a.title, a.text, a.score + b.score, a.sourceMask | 1, a.labels));
        }

        for (int rank = 0; rank < fullTextResults.size(); rank++) {
            SearchService.SearchResult r = fullTextResults.get(rank);
            Map<String, Object> src = r.source();
            Object docIdObj = src.get("doc_id");
            if (docIdObj == null) docIdObj = src.get("docId");
            if (docIdObj == null) continue;
            Long docId = ((Number) docIdObj).longValue();
            int chunkIdx = ((Number) src.getOrDefault("chunk_index", 0)).intValue();
            String key = chunkKey(docId, chunkIdx);
            String title = (String) src.get("title");
            String text = (String) src.get("text");
            List<String> labels = getLabelsFromMeta(src);

            double rrfScore = KEYWORD_WEIGHT * (1.0 / (RRF_K + rank + 1));
            rrfMap.merge(key, new RRFEntry(docId, chunkIdx, title, text, rrfScore, 2, labels),
                    (a, b) -> new RRFEntry(a.docId, a.chunkIndex, a.title, a.text, a.score + b.score, a.sourceMask | 2, a.labels));
        }
    }

    /** 混合检索结果语义去重：按余弦相似度过滤，保留前者去除后者 */
    private List<SearchResult> applySemanticDedup(List<SearchResult> merged) {
        if (merged.size() <= 1) return merged;
        List<String> texts = merged.stream().map(SearchResult::content).map(c -> c != null ? c : "").toList();
        List<List<Float>> embeddings;
        try {
            embeddings = vectorService.generateEmbeddings(texts);
        } catch (Exception e) {
            log.warn("语义去重 embedding 失败，跳过: {}", e.getMessage());
            return merged;
        }
        if (embeddings == null || embeddings.size() != merged.size()) return merged;
        List<Integer> keepIndex = new ArrayList<>();
        for (int i = 0; i < merged.size(); i++) {
            boolean duplicate = false;
            for (int j : keepIndex) {
                if (cosine(embeddings.get(j), embeddings.get(i)) >= hybridDedupThreshold) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) keepIndex.add(i);
        }
        return keepIndex.stream().map(merged::get).collect(Collectors.toList());
    }

    private static double cosine(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size()) return 0;
        double sum = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            float x = a.get(i), y = b.get(i);
            sum += x * y;
            normA += x * x;
            normB += y * y;
        }
        if (normA <= 0 || normB <= 0) return 0;
        return sum / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static String truncateQuery(String q) {
        if (q == null) return null;
        return q.length() <= 200 ? q : q.substring(0, 200) + "...";
    }

    private static String chunkKey(Long docId, int chunkIndex) {
        return docId + "_" + chunkIndex;
    }

    /** 检索缓存 key：含 kbId 前缀，便于导入后按库失效 */
    private static String cacheKey(Long kbId, String query, int topK) {
        String raw = "kb:search:" + kbId + ":" + query + ":" + topK;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return "cache:kb:" + kbId + ":" + sb.substring(0, 32);
        } catch (Exception e) {
            return "cache:kb:" + kbId + ":" + raw.hashCode();
        }
    }

    /** 导入/上传后清除该知识库检索缓存，使新文档立即可被非 RAG 检索命中 */
    @Override
    public void invalidateSearchCache(Long kbId) {
        if (kbId == null || stringRedisTemplate == null) return;
        try {
            Set<String> keys = stringRedisTemplate.keys("cache:kb:" + kbId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.debug("已清除知识库 {} 检索缓存，共 {} 条", kbId, keys.size());
            }
        } catch (Exception e) {
            log.warn("清除知识库检索缓存失败: kbId={}, {}", kbId, e.getMessage());
        }
    }

    private record RRFEntry(long docId, int chunkIndex, String title, String text, double score, int sourceMask, List<String> labels) {}

    @SuppressWarnings("unchecked")
    private static List<String> getLabelsFromMeta(Map<String, Object> meta) {
        if (meta == null) return List.of();
        Object o = meta.get("labels");
        if (o instanceof List<?> list) {
            return list.stream().filter(x -> x != null).map(String::valueOf).toList();
        }
        return List.of();
    }

    /** P2 缓存命中率统计：stats:kb:cache:hit / stats:kb:cache:miss */
    private void incrementCacheStat(String type) {
        if (stringRedisTemplate == null) return;
        try {
            stringRedisTemplate.opsForValue().increment("stats:kb:cache:" + type);
        } catch (Exception ignored) {
        }
    }

    /** 智能分块：512 字符，重叠 50，尽量在句子边界切分 */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) return chunks;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());

            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastExcl = text.lastIndexOf('！', end);
                int lastQuest = text.lastIndexOf('？', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int boundary = Math.max(Math.max(lastPeriod, lastExcl), Math.max(lastQuest, lastNewline));

                if (boundary > start + CHUNK_SIZE / 2) {
                    end = boundary + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }

        return chunks;
    }
}
