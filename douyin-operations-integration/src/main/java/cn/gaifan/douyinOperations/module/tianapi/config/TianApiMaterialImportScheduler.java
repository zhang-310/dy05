package cn.gaifan.douyinOperations.module.tianapi.config;

import cn.gaifan.douyinOperations.module.tianapi.service.TianApiMaterialImportService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TianAPI 文案素材自动入库定时任务。
 * 全量按类 HTTP 次数（如每类 1w）时单次可能数小时，提交到线程池异步执行，避免占用调度线程。
 */
@Component
@ConditionalOnProperty(prefix = "tianapi", name = "material-import-enabled", havingValue = "true")
public class TianApiMaterialImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(TianApiMaterialImportScheduler.class);

    @Resource
    private TianApiMaterialImportService materialImportService;

    @Resource
    @Qualifier("taskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Scheduled(cron = "${tianapi.material-import-cron:0 0 2 * * ?}")
    public void runImport() {
        taskExecutor.execute(() -> {
            try {
                log.info("TianAPI 素材自动入库任务开始（异步）");
                Map<String, Object> result = materialImportService.runImport();
                int imported = (Integer) result.getOrDefault("totalImported", 0);
                int skipped = (Integer) result.getOrDefault("totalSkipped", 0);
                int calls = (Integer) result.getOrDefault("totalCalls", 0);
                log.info("TianAPI 素材自动入库完成: imported={}, skipped={}, apiCalls={}", imported, skipped, calls);
            } catch (Exception e) {
                log.error("TianAPI 素材自动入库失败", e);
            }
        });
    }
}
