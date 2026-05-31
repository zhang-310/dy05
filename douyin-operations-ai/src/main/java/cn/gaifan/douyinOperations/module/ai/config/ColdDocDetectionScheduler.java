package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.ColdDocDetectionService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 冷门文档检测定时任务：每周扫描 90 天未检索/引用的文档，打日志供归档建议。
 */
@Component
public class ColdDocDetectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ColdDocDetectionScheduler.class);

    @Value("${app.ai.cold-doc.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.cold-doc.cold-days:90}")
    private int coldDays;

    @Value("${app.ai.cold-doc.max-log:50}")
    private int maxLog;

    /** 是否自动归档冷门文档（默认关闭，建议手动归档） */
    @Value("${app.ai.cold-doc.auto-archive-enabled:false}")
    private boolean autoArchiveEnabled;

    @Resource
    private ColdDocDetectionService coldDocDetectionService;

    @Scheduled(cron = "${app.ai.cold-doc.cron:0 0 3 * * SUN}")
    public void runDetection() {
        if (!enabled) {
            log.debug("冷门文档检测已禁用，跳过");
            return;
        }
        try {
            Map<String, Object> result = coldDocDetectionService.detectColdDocs(coldDays, maxLog);
            int count = (Integer) result.getOrDefault("coldCount", 0);
            if (count > 0) {
                if (autoArchiveEnabled) {
                    @SuppressWarnings("unchecked")
                    List<Long> docIds = ((List<?>) result.getOrDefault("coldDocIds", List.of())).stream()
                            .filter(n -> n instanceof Number)
                            .map(n -> ((Number) n).longValue())
                            .collect(Collectors.toList());
                    if (!docIds.isEmpty()) {
                        Map<String, Object> archiveResult = coldDocDetectionService.archiveColdDocs(docIds);
                        log.info("冷门文档自动归档: archived={}, failed={}",
                                archiveResult.get("archived"), archiveResult.get("failed"));
                    }
                } else {
                    log.warn("冷门文档检测: {} 个文档 90 天未检索/引用，建议归档复查，docIds={}",
                            count, result.get("coldDocIds"));
                }
            }
        } catch (Exception e) {
            log.error("冷门文档检测失败", e);
        }
    }
}
