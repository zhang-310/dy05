package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.KbDocumentQualityService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * E-3：知识库文档质量启发式扫描（与检索反馈、boost 闭环配合）。
 */
@Component
public class KbDocumentQualityScheduler {

    private static final Logger log = LoggerFactory.getLogger(KbDocumentQualityScheduler.class);

    @Value("${app.ai.kb-quality.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.kb-quality.scan-page-size:200}")
    private int scanPageSize;

    @Value("${app.ai.kb-quality.scan-max-pages:50}")
    private int scanMaxPages;

    @Resource
    private KbDocumentQualityService kbDocumentQualityService;

    @Scheduled(cron = "${app.ai.kb-quality.scan-cron:0 0 4 * * MON}")
    public void runHeuristicScan() {
        if (!enabled) {
            log.debug("KbDocument 质量扫描已禁用");
            return;
        }
        int total = 0;
        int maxPages = Math.max(1, Math.min(scanMaxPages, 500));
        try {
            for (int p = 0; p < maxPages; p++) {
                int n = kbDocumentQualityService.scanHeuristicPage(p, scanPageSize);
                if (n < 0) {
                    break;
                }
                total += n;
            }
            if (total > 0) {
                log.info("KbDocument 启发式质量扫描完成：累计更新 {} 条（最多 {} 页 × {} 条/页）", total, maxPages, scanPageSize);
            }
        } catch (Exception e) {
            log.warn("KbDocument 质量扫描失败: {}", e.getMessage());
        }
    }
}
