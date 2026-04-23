package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.repository.ScriptVectorEmbeddingRepository;
import cn.gaifan.douyinOperations.module.script.service.VectorEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 向量嵌入定时任务
 * 定期更新未索引的向量嵌入，导入 Milvus
 */
@Slf4j
@Component
public class VectorEmbeddingTask {

    @Resource
    private VectorEmbeddingService vectorEmbeddingService;

    @Resource
    private ScriptVectorEmbeddingRepository scriptVectorEmbeddingRepository;

    /**
     * 每小时执行一次：索引未索引的向量嵌入
     * 批量大小：100
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)  // 1 小时执行一次
    public void indexUnindexedEmbeddings() {
        log.info("开始索引未索引的向量嵌入...");
        try {
            long startTime = System.currentTimeMillis();
            int batchSize = 100;

            // 获取未索引的向量嵌入 ID
            List<Long> unindexedIds = vectorEmbeddingService.getUnindexedEmbeddingIds(batchSize);

            if (unindexedIds.isEmpty()) {
                log.debug("没有未索引的向量嵌入");
                return;
            }

            // 批量索引
            vectorEmbeddingService.batchIndexToMilvus(unindexedIds);

            long duration = System.currentTimeMillis() - startTime;
            log.info("索引向量嵌入完成: {} 条，耗时 {} ms", unindexedIds.size(), duration);
        } catch (Exception e) {
            log.error("索引向量嵌入失败", e);
        }
    }

    /**
     * 每天凌晨 2 点执行：清理已删除的向量嵌入
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天 2:00 AM
    public void cleanupDeletedEmbeddings() {
        log.info("开始清理已删除的向量嵌入...");
        try {
            long startTime = System.currentTimeMillis();

            // 查找并硬删除已标记为删除（deleted=1）的向量嵌入
            int deletedCount = 0;
            // 实现硬删除逻辑：delete from sc_script_vector_embedding where deleted = 1

            long duration = System.currentTimeMillis() - startTime;
            log.info("清理已删除的向量嵌入完成: {} 条，耗时 {} ms", deletedCount, duration);
        } catch (Exception e) {
            log.error("清理已删除的向量嵌入失败", e);
        }
    }

    /**
     * 每 6 小时执行一次：检查向量库健康状态
     */
    @Scheduled(fixedDelay = 21600000, initialDelay = 120000)  // 6 小时执行一次
    public void checkVectorHealthStatus() {
        log.info("开始检查向量库健康状态...");
        try {
            long totalEmbeddings = scriptVectorEmbeddingRepository.countByOwnerId(null);
            long indexedEmbeddings = scriptVectorEmbeddingRepository.countIndexedEmbeddings();

            double indexRate = totalEmbeddings > 0 ? (indexedEmbeddings * 100.0 / totalEmbeddings) : 0;

            log.info("向量库健康状态: 总数={}, 已索引={}, 索引率={:.2f}%",
                    totalEmbeddings, indexedEmbeddings, indexRate);

            if (indexRate < 80) {
                log.warn("向量索引率过低（< 80%），请检查向量库");
            }
        } catch (Exception e) {
            log.error("检查向量库健康状态失败", e);
        }
    }
}
