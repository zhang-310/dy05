package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

/**
 * P0 双写补偿：定时扫描 status=0 且超时的文档，重试 Milvus+ES 写入。
 * 支持管理员在「系统配置」中动态修改 ai.dual-write.compensation.batch-size。
 */
@Component
public class DualWriteCompensationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DualWriteCompensationScheduler.class);

    @Value("${app.ai.dual-write.compensation.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.dual-write.compensation.stuck-minutes:10}")
    private int stuckMinutes;

    @Value("${app.ai.dual-write.compensation.max-retry-count:12}")
    private int maxRetryCount;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private AiRuntimeConfig aiRuntimeConfig;

    @Scheduled(cron = "${app.ai.dual-write.compensation.cron:0 */30 * * * ?}")
    public void runCompensation() {
        if (!enabled) return;
        int batchSize = aiRuntimeConfig.getDualWriteBatchSize();
        long cutoff = System.currentTimeMillis() - (long) stuckMinutes * 60 * 1000;
        int cappedMaxRetry = Math.max(4, maxRetryCount);
        List<AiKbDocument> stuck = documentRepository.findStuckForCompensation(
                new Timestamp(cutoff), cappedMaxRetry, PageRequest.of(0, batchSize));
        if (stuck.isEmpty()) return;

        int ok = 0;
        for (AiKbDocument doc : stuck) {
            try {
                ok += knowledgeBaseService.retrySyncDocument(doc);
            } catch (Exception e) {
                log.warn("补偿失败 docId={}: {}", doc.getId(), e.getMessage());
            }
        }
        if (ok > 0) log.info("双写补偿完成: 成功 {} 条", ok);
    }
}
