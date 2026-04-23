package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.CrossDomainShareService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 跨域共享 Agent 实现（Phase 4.5 重写）
 * 高分知识沉淀、LLM 适配、质量验证、公共知识库
 */
@Service
public class CrossDomainShareServiceImpl implements CrossDomainShareService {

    private static final Logger log = LoggerFactory.getLogger(CrossDomainShareServiceImpl.class);
    private static final long MIN_QUALITY_CITATION = 5L;   // 质量>80 等价
    private static final long MIN_EFFECTIVENESS_CITATION = 3L;
    private static final long MIN_USAGE = 10L;  // retrieval + citation
    private static final String SOURCE_TYPE_CROSS_SHARE = "cross_share";
    private static final int MIN_CONTENT_LENGTH = 50;

    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Resource
    private AiKbDocumentRepository documentRepository;
    @Resource
    private KnowledgeBaseService knowledgeBaseService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private LlmClient llmClient;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runCrossDomainShare(Long userId, int maxShares) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sharedCount", 0);
        result.put("skippedCount", 0);
        result.put("adaptedCount", 0);
        result.put("errors", new ArrayList<String>());

        List<AiKnowledgeBase> kbs = knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
        if (kbs.size() < 2) {
            result.put("reason", "用户仅有 0 或 1 个知识库，无需跨库共享");
            return result;
        }

        int shared = 0;
        int skipped = 0;
        int adapted = 0;
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");

        for (AiKnowledgeBase sourceKb : kbs) {
            if (shared >= maxShares) break;
            List<AiKbDocument> shareable = getHighQualityShareableDocs(sourceKb.getId(), Math.min(20, maxShares - shared + 5));
            if (shareable.isEmpty()) continue;

            List<AiKnowledgeBase> targetKbs = kbs.stream()
                    .filter(kb -> !kb.getId().equals(sourceKb.getId()))
                    .filter(kb -> kb.getKbName() == null || !"huashu".equalsIgnoreCase(kb.getKbName()))
                    .collect(Collectors.toList());

            for (AiKbDocument doc : shareable) {
                if (shared >= maxShares) break;
                if (doc.getContent() == null || doc.getContent().length() < MIN_CONTENT_LENGTH) {
                    skipped++;
                    continue;
                }
                String title = doc.getTitle() != null ? doc.getTitle() : "共享-" + doc.getId();
                String content = doc.getContent();

                for (AiKnowledgeBase targetKb : targetKbs) {
                    if (shared >= maxShares) break;
                    try {
                        String adaptedContent = content;
                        if (needsAdaptation(doc, sourceKb, targetKb)) {
                            adaptedContent = adaptContentViaLlm(content, doc.getSourceType(), sourceKb.getKbType(), targetKb.getKbType());
                            if (adaptedContent != null && adaptedContent.length() >= MIN_CONTENT_LENGTH) {
                                adapted++;
                            }
                        }
                        if (adaptedContent == null) adaptedContent = content;
                        if (validateContent(adaptedContent)) {
                            knowledgeBaseService.uploadDocument(
                                    targetKb.getId(), title, adaptedContent, "md", userId, SOURCE_TYPE_CROSS_SHARE);
                            shared++;
                            log.info("跨域共享成功: docId={} -> targetKb={}", doc.getId(), targetKb.getId());
                        } else {
                            skipped++;
                        }
                    } catch (Exception e) {
                        errors.add("docId=" + doc.getId() + " targetKb=" + targetKb.getId() + ": " + e.getMessage());
                        log.warn("跨域共享失败: docId={}, targetKb={}: {}", doc.getId(), targetKb.getId(), e.getMessage());
                    }
                }
            }
        }
        result.put("sharedCount", shared);
        result.put("skippedCount", skipped);
        result.put("adaptedCount", adapted);
        return result;
    }

    private List<AiKbDocument> getHighQualityShareableDocs(Long kbId, int topN) {
        List<AiKbDocument> merged = new ArrayList<>();
        for (String sourceType : List.of("evolved", "viral_analysis", "live_review", "live_script")) {
            merged.addAll(documentRepository.findTopCitedByKbAndSourceType(
                    kbId, sourceType, MIN_EFFECTIVENESS_CITATION, PageRequest.of(0, topN)));
        }
        merged = merged.stream()
                .distinct()
                .filter(d -> {
                    long usage = (d.getRetrievalCount() != null ? d.getRetrievalCount() : 0) + (d.getCitationCount() != null ? d.getCitationCount() : 0);
                    return usage >= MIN_USAGE && (d.getCitationCount() != null && d.getCitationCount() >= MIN_QUALITY_CITATION);
                })
                .sorted((a, b) -> Long.compare(
                        (b.getCitationCount() != null ? b.getCitationCount() : 0) * 2 + (b.getRetrievalCount() != null ? b.getRetrievalCount() : 0),
                        (a.getCitationCount() != null ? a.getCitationCount() : 0) * 2 + (a.getRetrievalCount() != null ? a.getRetrievalCount() : 0)))
                .limit(topN)
                .toList();
        return merged;
    }

    private boolean needsAdaptation(AiKbDocument doc, AiKnowledgeBase source, AiKnowledgeBase target) {
        if (source.getKbType() == null || target.getKbType() == null) return false;
        if (source.getKbType().equals(target.getKbType())) return false;
        String st = doc.getSourceType();
        return "evolved".equals(st) || "live_review".equals(st) || "live_script".equals(st);
    }

    private String adaptContentViaLlm(String content, String sourceType, String sourceKbType, String targetKbType) {
        if (llmClient == null || aiModelRepository == null) return content;
        try {
            var models = aiModelRepository.findByStatusAndDeleted(1, 0);
            if (models.isEmpty()) return content;
            String system = "你是知识迁移助手。将行业知识通用化，保留结构和节奏，替换产品名等为通用表述。只输出改写后的内容，不要解释。";
            String prompt = String.format("原内容（来源：%s，类型：%s）：\n%s\n\n请改写为适合 %s 类型知识库的通用表述：", sourceType, sourceKbType, content.substring(0, Math.min(2000, content.length())), targetKbType);
            var resp = llmClient.chat(models.get(0), system, prompt);
            if (resp != null && resp.success() && resp.content() != null && !resp.content().isBlank()) {
                return resp.content().trim();
            }
        } catch (Exception e) {
            log.debug("LLM 适配失败: {}", e.getMessage());
        }
        return content;
    }

    private boolean validateContent(String content) {
        if (content == null || content.length() < MIN_CONTENT_LENGTH) return false;
        if (content.length() > 100_000) return false;
        return true;
    }

    @Override
    public List<Map<String, Object>> getShareableDocs(Long kbId, int topN) {
        List<AiKbDocument> docs = getHighQualityShareableDocs(kbId, topN);
        if (docs.isEmpty()) {
            for (String sourceType : List.of("evolved", "viral_analysis", "live_review")) {
                docs.addAll(documentRepository.findTopCitedByKbAndSourceType(
                        kbId, sourceType, 2L, PageRequest.of(0, topN * 2)));
            }
            docs = docs.stream().distinct()
                    .sorted((a, b) -> Long.compare(
                            b.getCitationCount() != null ? b.getCitationCount() : 0L,
                            a.getCitationCount() != null ? a.getCitationCount() : 0L))
                    .limit(topN)
                    .toList();
        }
        return docs.stream()
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("docId", d.getId());
                    m.put("title", d.getTitle());
                    m.put("citationCount", d.getCitationCount());
                    m.put("retrievalCount", d.getRetrievalCount());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * 定时任务：高分知识沉淀到公共知识库（owner_id=0）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runWeeklyPublicSink(int maxDocs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sinkedCount", 0);
        result.put("skippedCount", 0);
        result.put("errors", new ArrayList<String>());

        Optional<AiKnowledgeBase> publicKbOpt = knowledgeBaseRepository.findByUserIdAndKbNameAndDeleted(0L, "公共知识库", 0);
        AiKnowledgeBase publicKb;
        if (publicKbOpt.isEmpty()) {
            try {
                publicKb = knowledgeBaseService.createKnowledgeBase("公共知识库", "系统自动沉淀的高分知识", 0L);
            } catch (Exception e) {
                log.warn("创建公共知识库失败: {}", e.getMessage());
                result.put("reason", "公共知识库创建失败");
                return result;
            }
        } else {
            publicKb = publicKbOpt.get();
        }

        List<Long> userIds = knowledgeBaseRepository.findDistinctUserIds();
        int sinked = 0;
        for (Long userId : userIds) {
            if (userId == null || userId == 0) continue;
            if (sinked >= maxDocs) break;
            for (AiKnowledgeBase kb : knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0)) {
                if (sinked >= maxDocs) break;
                for (AiKbDocument doc : getHighQualityShareableDocs(kb.getId(), 5)) {
                    if (sinked >= maxDocs) break;
                    if (doc.getContent() == null || doc.getContent().length() < MIN_CONTENT_LENGTH) continue;
                    try {
                        String title = "[沉淀] " + (doc.getTitle() != null ? doc.getTitle() : "共享-" + doc.getId());
                        String content = doc.getContent();
                        if (content.length() > 5000) content = content.substring(0, 5000) + "\n...(截断)";
                        knowledgeBaseService.uploadDocument(publicKb.getId(), title, content, "md", 0L, "public_sink");
                        sinked++;
                        log.info("公共知识库沉淀: docId={}", doc.getId());
                    } catch (Exception e) {
                        ((List<String>) result.get("errors")).add("docId=" + doc.getId() + ": " + e.getMessage());
                    }
                }
            }
        }
        result.put("sinkedCount", sinked);
        return result;
    }
}
