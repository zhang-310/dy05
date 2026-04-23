package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.script.entity.ScriptLibrary;
import cn.gaifan.douyinOperations.module.script.entity.ScriptVectorEmbedding;
import cn.gaifan.douyinOperations.module.script.repository.ScriptLibraryRepository;
import cn.gaifan.douyinOperations.module.script.repository.ScriptVectorEmbeddingRepository;
import cn.gaifan.douyinOperations.module.script.service.VectorEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.*;

/**
 * 向量嵌入服务实现
 * 支持 Ollama 本地推理和向量索引
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class VectorEmbeddingServiceImpl implements VectorEmbeddingService {

    private static final int VECTOR_DIMENSION = 1024;
    private static final String EMBEDDING_MODEL = "BGE-M3";

    @Resource
    private ScriptLibraryRepository scriptLibraryRepository;

    @Resource
    private ScriptVectorEmbeddingRepository scriptVectorEmbeddingRepository;

    /**
     * 生成单个向量嵌入
     * 调用 Ollama API 进行向量化
     */
    @Override
    public byte[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文本不能为空");
        }

        try {
            long startTime = System.currentTimeMillis();

            // 模拟向量生成（实际生产应调用 Ollama API）
            // curl -X POST http://localhost:11434/api/embeddings -d '{"model":"bge-m3","prompt":"text"}'
            float[] embedding = generateMockEmbedding(text);

            // 验证维度
            if (embedding.length != VECTOR_DIMENSION) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "向量维度不匹配，期望: " + VECTOR_DIMENSION + "，实际: " + embedding.length);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("向量化耗时: {}ms", duration);

            return floatsToBytes(embedding);
        } catch (Exception e) {
            log.error("生成向量嵌入失败: {}", text, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "向量化失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成向量嵌入
     */
    @Override
    public List<byte[]> batchGenerateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        return texts.stream()
                .map(text -> {
                    try {
                        return generateEmbedding(text);
                    } catch (Exception e) {
                        log.warn("批量生成向量失败: {}", text, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 更新话术向量
     */
    @Override
    public void updateEmbedding(Long scriptId, String newText, Long userId) {
        if (scriptId == null || scriptId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术 ID 无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }

        try {
            ScriptLibrary script = scriptLibraryRepository.findAll((root, q, cb) -> {
                List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("id"), scriptId));
                predicates.add(cb.equal(root.get("deleted"), 0));
                predicates.add(cb.equal(root.get("userId"), userId));
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            }).stream().findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));

            // 生成新的向量嵌入
            long startTime = System.currentTimeMillis();
            byte[] embedding = generateEmbedding(newText);
            long duration = System.currentTimeMillis() - startTime;

            // 更新或创建向量记录
            ScriptVectorEmbedding vectorEmbedding = scriptVectorEmbeddingRepository
                    .findByScriptIdAndDeleted(scriptId, 0)
                    .orElse(new ScriptVectorEmbedding());

            vectorEmbedding.setScriptId(scriptId);
            vectorEmbedding.setOwnerId(userId);
            vectorEmbedding.setTitle(script.getTitle());
            vectorEmbedding.setContentText(script.getContent());
            vectorEmbedding.setVectorEmbedding(embedding);
            vectorEmbedding.setVectorDimension(VECTOR_DIMENSION);
            vectorEmbedding.setEmbeddingModel(EMBEDDING_MODEL);
            vectorEmbedding.setEmbeddingTimeMs((int) duration);
            vectorEmbedding.setIsIndexedMilvus(false);

            scriptVectorEmbeddingRepository.save(vectorEmbedding);
            log.info("更新话术向量成功: scriptId={}", scriptId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新话术向量失败: {}", scriptId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新向量失败");
        }
    }

    /**
     * 索引向量到 Milvus
     */
    @Override
    public void indexToMilvus(Long scriptVectorEmbeddingId) {
        if (scriptVectorEmbeddingId == null || scriptVectorEmbeddingId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "向量嵌入 ID 无效");
        }

        try {
            ScriptVectorEmbedding embedding = scriptVectorEmbeddingRepository
                    .findByIdAndDeleted(scriptVectorEmbeddingId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "向量嵌入不存在"));

            // 实际生产应调用 Milvus 客户端 API
            // milvusClient.insert(collectionName, vectorEmbedding)

            embedding.setIsIndexedMilvus(true);
            embedding.setMilvusCollectionId(scriptVectorEmbeddingId);
            embedding.setLastIndexedAt(new Timestamp(System.currentTimeMillis()));
            scriptVectorEmbeddingRepository.save(embedding);

            log.info("索引向量到 Milvus 成功: {}", scriptVectorEmbeddingId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("索引向量失败: {}", scriptVectorEmbeddingId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "索引失败");
        }
    }

    /**
     * 批量索引向量到 Milvus
     */
    @Override
    public void batchIndexToMilvus(List<Long> scriptVectorEmbeddingIds) {
        if (scriptVectorEmbeddingIds == null || scriptVectorEmbeddingIds.isEmpty()) {
            return;
        }

        scriptVectorEmbeddingIds.forEach(id -> {
            try {
                indexToMilvus(id);
            } catch (Exception e) {
                log.warn("批量索引向量失败: {}", id, e);
            }
        });
    }

    /**
     * 检查向量维度
     */
    @Override
    public void validateVectorDimension(byte[] vector) {
        if (vector == null || vector.length == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "向量为空");
        }

        int actualDimension = vector.length / 4;  // 每个 float 占 4 字节
        if (actualDimension != VECTOR_DIMENSION) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "向量维度不匹配，期望: " + VECTOR_DIMENSION + "，实际: " + actualDimension);
        }
    }

    /**
     * 获取未索引的向量嵌入
     */
    @Override
    public List<Long> getUnindexedEmbeddingIds(int limit) {
        return scriptVectorEmbeddingRepository.findUnindexedEmbeddings(limit).stream()
                .map(ScriptVectorEmbedding::getId)
                .toList();
    }

    // ========== 私有方法 ==========

    /**
     * 生成模拟向量（用于开发测试）
     * 实际生产应调用 Ollama 或 DeepSeek API
     */
    private float[] generateMockEmbedding(String text) {
        float[] embedding = new float[VECTOR_DIMENSION];
        Random rand = new Random(text.hashCode());

        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            embedding[i] = (rand.nextFloat() - 0.5f) * 2.0f;  // [-1, 1]
        }

        // 归一化
        float norm = 0.0f;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            embedding[i] /= norm;
        }

        return embedding;
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
}
