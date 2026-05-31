package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.AiEmbeddingConstants;
import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkQualityScriptRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkScriptSimilarityService;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

/**
 * 质量脚本语义相似度服务实现
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BenchmarkScriptSimilarityServiceImpl implements BenchmarkScriptSimilarityService {

    private static final String COLLECTION_NAME = "benchmark_quality_scripts";
    private static final int VECTOR_DIMENSION = AiEmbeddingConstants.VECTOR_DIMENSION;
    private static final String EMBEDDING_MODEL = "BGE-M3";

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Autowired
    private BenchmarkQualityScriptRepository qualityScriptRepository;

    @Autowired(required = false)
    private VectorService vectorService;

    /**
     * 为质量脚本生成向量嵌入
     */
    @Override
    public void generateEmbedding(Long scriptId, Long ownerId) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 开始生成向量嵌入: scriptId={}, ownerId={}", traceId, scriptId, ownerId);

        try {
            // 查询质量脚本
            BenchmarkQualityScript script = qualityScriptRepository.findById(scriptId)
                    .filter(s -> s.getOwnerId().equals(ownerId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "质量脚本不存在"));

            // 检查是否已有向量
            if (script.getEmbeddingVector() != null && !script.getEmbeddingVector().isEmpty()) {
                log.info("[{}] 脚本已有向量嵌入，跳过生成", traceId);
                return;
            }

            // 生成向量嵌入
            String text = script.getScriptContent();
            if (text == null || text.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本内容为空");
            }

            byte[] embedding = generateEmbeddingFromText(text);

            // 保存向量到数据库（PostgreSQL VECTOR 类型）
            script.setEmbeddingVector(bytesToVectorString(embedding));
            qualityScriptRepository.save(script);

            log.info("[{}] 向量嵌入生成成功: scriptId={}", traceId, scriptId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] 生成向量嵌入失败: scriptId={}, error={}", traceId, scriptId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成向量嵌入失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成向量嵌入
     */
    @Override
    public Integer batchGenerateEmbeddings(List<Long> scriptIds, Long ownerId) {
        if (scriptIds == null || scriptIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Long scriptId : scriptIds) {
            try {
                generateEmbedding(scriptId, ownerId);
                successCount++;
            } catch (Exception e) {
                log.warn("批量生成向量失败: scriptId={}, error={}", scriptId, e.getMessage());
            }
        }

        log.info("批量生成向量完成: total={}, success={}", scriptIds.size(), successCount);
        return successCount;
    }

    /**
     * 索引向量到 Milvus
     */
    @Override
    public void indexToMilvus(Long scriptId, Long ownerId) {
        if (milvusClient == null) {
            log.warn("Milvus 客户端未配置，跳过索引");
            return;
        }

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 开始索引向量到 Milvus: scriptId={}", traceId, scriptId);

        try {
            // 查询质量脚本
            BenchmarkQualityScript script = qualityScriptRepository.findById(scriptId)
                    .filter(s -> s.getOwnerId().equals(ownerId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "质量脚本不存在"));

            // 检查是否有向量
            if (script.getEmbeddingVector() == null || script.getEmbeddingVector().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本尚未生成向量嵌入");
            }

            // 确保集合存在
            ensureCollectionExists();

            // 准备数据
            List<Long> ids = Collections.singletonList(scriptId);
            List<List<Float>> vectors = Collections.singletonList(vectorStringToFloatList(script.getEmbeddingVector()));

            // 插入到 Milvus
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(Arrays.asList(
                            new InsertParam.Field("id", ids),
                            new InsertParam.Field("embedding", vectors)
                    ))
                    .build();

            R<io.milvus.grpc.MutationResult> response = milvusClient.insert(insertParam);
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Milvus 插入失败: " + response.getMessage());
            }

            log.info("[{}] 向量索引成功: scriptId={}", traceId, scriptId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] 索引向量失败: scriptId={}, error={}", traceId, scriptId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "索引向量失败: " + e.getMessage());
        }
    }

    /**
     * 批量索引向量到 Milvus
     */
    @Override
    public Integer batchIndexToMilvus(List<Long> scriptIds, Long ownerId) {
        if (scriptIds == null || scriptIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Long scriptId : scriptIds) {
            try {
                indexToMilvus(scriptId, ownerId);
                successCount++;
            } catch (Exception e) {
                log.warn("批量索引向量失败: scriptId={}, error={}", scriptId, e.getMessage());
            }
        }

        log.info("批量索引向量完成: total={}, success={}", scriptIds.size(), successCount);
        return successCount;
    }

    /**
     * 查找相似脚本
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> findSimilarScripts(Long scriptId, Long ownerId, Integer topK, Double minScore) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 开始查找相似脚本: scriptId={}, topK={}, minScore={}", traceId, scriptId, topK, minScore);

        try {
            // 查询质量脚本
            BenchmarkQualityScript script = qualityScriptRepository.findById(scriptId)
                    .filter(s -> s.getOwnerId().equals(ownerId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "质量脚本不存在"));

            // 检查是否有向量
            if (script.getEmbeddingVector() == null || script.getEmbeddingVector().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本尚未生成向量嵌入");
            }

            // 使用 Milvus 搜索
            if (milvusClient != null) {
                return searchSimilarScriptsInMilvus(script.getEmbeddingVector(), ownerId, topK, minScore);
            }

            // 降级：使用数据库余弦相似度计算
            return searchSimilarScriptsInDatabase(script.getEmbeddingVector(), scriptId, ownerId, topK, minScore);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] 查找相似脚本失败: scriptId={}, error={}", traceId, scriptId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查找相似脚本失败: " + e.getMessage());
        }
    }

    /**
     * 根据文本查找相似脚本
     */
    @Override
    public List<BenchmarkScriptSimilarityVO> findSimilarScriptsByText(String text, Long ownerId, Integer topK, Double minScore) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 开始根据文本查找相似脚本: textLength={}, topK={}", traceId, text.length(), topK);

        try {
            // 生成文本向量
            byte[] embedding = generateEmbeddingFromText(text);
            String vectorString = bytesToVectorString(embedding);

            // 使用 Milvus 搜索
            if (milvusClient != null) {
                return searchSimilarScriptsInMilvus(vectorString, ownerId, topK, minScore);
            }

            // 降级：使用数据库余弦相似度计算
            return searchSimilarScriptsInDatabase(vectorString, null, ownerId, topK, minScore);

        } catch (Exception e) {
            log.error("[{}] 根据文本查找相似脚本失败: error=", traceId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查找相似脚本失败: " + e.getMessage());
        }
    }

    /**
     * 计算两个脚本的相似度
     */
    @Override
    public Double calculateSimilarity(Long scriptId1, Long scriptId2, Long ownerId) {
        try {
            BenchmarkQualityScript script1 = qualityScriptRepository.findById(scriptId1)
                    .filter(s -> s.getOwnerId().equals(ownerId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本1不存在"));

            BenchmarkQualityScript script2 = qualityScriptRepository.findById(scriptId2)
                    .filter(s -> s.getOwnerId().equals(ownerId))
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "脚本2不存在"));

            if (script1.getEmbeddingVector() == null || script2.getEmbeddingVector() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "脚本尚未生成向量嵌入");
            }

            // 计算余弦相似度
            List<Float> vec1 = vectorStringToFloatList(script1.getEmbeddingVector());
            List<Float> vec2 = vectorStringToFloatList(script2.getEmbeddingVector());

            return calculateCosineSimilarity(vec1, vec2);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("计算相似度失败: scriptId1={}, scriptId2={}, error={}", scriptId1, scriptId2, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "计算相似度失败: " + e.getMessage());
        }
    }

    /**
     * 获取未生成向量的脚本 ID 列表
     */
    @Override
    public List<Long> getUnembeddedScriptIds(Long ownerId, Integer limit) {
        Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));
            predicates.add(cb.or(
                    cb.isNull(root.get("embeddingVector")),
                    cb.equal(root.get("embeddingVector"), "")
            ));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return qualityScriptRepository.findAll(spec).stream()
                .limit(limit != null ? limit : 100)
                .map(BenchmarkQualityScript::getId)
                .collect(Collectors.toList());
    }

    /**
     * 获取未索引到 Milvus 的脚本 ID 列表
     */
    @Override
    public List<Long> getUnindexedScriptIds(Long ownerId, Integer limit) {
        // 已有向量但未索引的脚本
        Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));
            predicates.add(cb.isNotNull(root.get("embeddingVector")));
            predicates.add(cb.notEqual(root.get("embeddingVector"), ""));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return qualityScriptRepository.findAll(spec).stream()
                .limit(limit != null ? limit : 100)
                .map(BenchmarkQualityScript::getId)
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 从文本生成向量嵌入
     */
    private byte[] generateEmbeddingFromText(String text) {
        if (vectorService == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Embedding 服务未配置，无法生成脚本向量");
        }
        List<Float> vector = vectorService.generateEmbedding(text);
        if (vector == null || vector.size() != VECTOR_DIMENSION) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "向量维度不匹配，期望: " + VECTOR_DIMENSION + "，实际: " + (vector == null ? 0 : vector.size()));
        }
        float[] embedding = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            embedding[i] = vector.get(i);
        }
        return floatsToBytes(embedding);
    }

    /**
     * Float 数组转字节数组
     */
    private byte[] floatsToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    /**
     * 字节数组转 PostgreSQL VECTOR 字符串格式
     */
    private String bytesToVectorString(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            if (i > 0) sb.append(",");
            sb.append(buffer.getFloat());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * PostgreSQL VECTOR 字符串转 Float 列表
     */
    private List<Float> vectorStringToFloatList(String vectorString) {
        // 格式: "[0.1,0.2,0.3,...]"
        String content = vectorString.substring(1, vectorString.length() - 1);
        String[] parts = content.split(",");
        List<Float> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(Float.parseFloat(part.trim()));
        }
        return result;
    }

    /**
     * 确保 Milvus 集合存在
     */
    private void ensureCollectionExists() {
        if (milvusClient == null) {
            return;
        }

        try {
            R<Boolean> hasCollection = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(COLLECTION_NAME)
                            .build()
            );

            if (hasCollection.getData() != null && hasCollection.getData()) {
                return;
            }

            // 创建集合
            FieldType idField = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType embeddingField = FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(VECTOR_DIMENSION)
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withDescription("Benchmark quality scripts embeddings")
                    .addFieldType(idField)
                    .addFieldType(embeddingField)
                    .build();

            milvusClient.createCollection(createParam);

            // 创建索引
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFieldName("embedding")
                    .withIndexType(io.milvus.param.IndexType.IVF_FLAT)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withExtraParam("{\"nlist\":128}")
                    .build();

            milvusClient.createIndex(indexParam);

            log.info("Milvus 集合创建成功: {}", COLLECTION_NAME);
        } catch (Exception e) {
            log.error("创建 Milvus 集合失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 在 Milvus 中搜索相似脚本
     */
    private List<BenchmarkScriptSimilarityVO> searchSimilarScriptsInMilvus(String vectorString, Long ownerId, Integer topK, Double minScore) {
        try {
            List<Float> queryVector = vectorStringToFloatList(vectorString);
            List<List<Float>> searchVectors = Collections.singletonList(queryVector);

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withVectorFieldName("embedding")
                    .withVectors(searchVectors)
                    .withTopK(topK != null ? topK : 10)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withParams("{\"nprobe\":10}")
                    .build();

            R<SearchResults> response = milvusClient.search(searchParam);
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Milvus 搜索失败: " + response.getMessage());
            }

            SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
            List<Long> scriptIds = new ArrayList<>();
            Map<Long, Double> scoreMap = new HashMap<>();

            for (int i = 0; i < wrapper.getIDScore(0).size(); i++) {
                Long scriptId = (Long) wrapper.getIDScore(0).get(i).getLongID();
                Double score = (double) wrapper.getIDScore(0).get(i).getScore();

                if (minScore == null || score >= minScore) {
                    scriptIds.add(scriptId);
                    scoreMap.put(scriptId, score);
                }
            }

            // 从数据库加载脚本详情
            return loadScriptDetails(scriptIds, scoreMap, ownerId);

        } catch (Exception e) {
            log.error("Milvus 搜索失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索失败: " + e.getMessage());
        }
    }

    /**
     * 在数据库中搜索相似脚本（降级方案）
     */
    private List<BenchmarkScriptSimilarityVO> searchSimilarScriptsInDatabase(String vectorString, Long excludeScriptId, Long ownerId, Integer topK, Double minScore) {
        // 查询所有有向量的脚本
        Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));
            predicates.add(cb.isNotNull(root.get("embeddingVector")));
            predicates.add(cb.notEqual(root.get("embeddingVector"), ""));
            if (excludeScriptId != null) {
                predicates.add(cb.notEqual(root.get("id"), excludeScriptId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAll(spec);
        List<Float> queryVector = vectorStringToFloatList(vectorString);

        // 计算相似度并排序
        List<BenchmarkScriptSimilarityVO> results = new ArrayList<>();
        for (BenchmarkQualityScript script : scripts) {
            List<Float> scriptVector = vectorStringToFloatList(script.getEmbeddingVector());
            Double similarity = calculateCosineSimilarity(queryVector, scriptVector);

            if (minScore == null || similarity >= minScore) {
                BenchmarkScriptSimilarityVO vo = convertToSimilarityVO(script, similarity);
                results.add(vo);
            }
        }

        // 按相似度降序排序
        results.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));

        // 限制返回数量
        int limit = topK != null ? topK : 10;
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 计算余弦相似度
     */
    private Double calculateCosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "向量维度不匹配");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 加载脚本详情
     */
    private List<BenchmarkScriptSimilarityVO> loadScriptDetails(List<Long> scriptIds, Map<Long, Double> scoreMap, Long ownerId) {
        if (scriptIds.isEmpty()) {
            return new ArrayList<>();
        }

        Specification<BenchmarkQualityScript> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));
            predicates.add(root.get("id").in(scriptIds));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<BenchmarkQualityScript> scripts = qualityScriptRepository.findAll(spec);

        return scripts.stream()
                .map(script -> {
                    Double score = scoreMap.get(script.getId());
                    return convertToSimilarityVO(script, score);
                })
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .collect(Collectors.toList());
    }

    /**
     * 转换为相似度 VO
     */
    private BenchmarkScriptSimilarityVO convertToSimilarityVO(BenchmarkQualityScript script, Double similarityScore) {
        BenchmarkScriptSimilarityVO vo = new BenchmarkScriptSimilarityVO();
        vo.setScriptId(script.getId());
        vo.setVideoId(script.getVideoId());
        vo.setScriptContent(truncateContent(script.getScriptContent(), 200));
        vo.setScriptType(script.getScriptType());
        vo.setIndustry(script.getIndustry());
        vo.setSceneType(script.getSceneType());
        vo.setQualityScore(script.getQualityScore());
        vo.setSimilarityScore(similarityScore);
        vo.setEngagementRate(script.getEngagementRate());
        vo.setViralScore(script.getViralScore());
        vo.setLikesCount(script.getLikesCount());
        vo.setCommentsCount(script.getCommentsCount());
        vo.setSharesCount(script.getSharesCount());
        vo.setCollectionsCount(script.getCollectionsCount());
        vo.setViewsCount(script.getViewsCount());
        return vo;
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
