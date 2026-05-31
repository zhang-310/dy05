package cn.gaifan.douyinOperations.module.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * 清理 {@code app.video-analysis.work-dir} 下过期的下载视频、抽帧、音频、转写等临时文件。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.video-analysis.enabled", havingValue = "true", matchIfMissing = false)
public class VideoAnalysisTempCleanupScheduler {

    @Value("${app.video-analysis.work-dir:/tmp/video-analysis}")
    private String workDir;

    @Value("${app.video-analysis.temp-cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.video-analysis.temp-cleanup.max-age-hours:24}")
    private int maxAgeHours;

    @Scheduled(cron = "${app.video-analysis.temp-cleanup.cron:0 15 4 * * ?}")
    public void cleanupOldTempFiles() {
        if (!cleanupEnabled || maxAgeHours <= 0) {
            return;
        }
        Path root = Paths.get(workDir);
        if (!Files.isDirectory(root)) {
            return;
        }
        long cutoff = System.currentTimeMillis() - maxAgeHours * 3600_000L;
        AtomicLong deletedBytes = new AtomicLong();
        AtomicLong deletedFiles = new AtomicLong();
        String[] subdirs = {"videos", "frames", "audio", "transcripts", "workflow", "compose", "sfx", "music", "digital-human"};
        for (String sub : subdirs) {
            Path dir = root.resolve(sub);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile).forEach(p -> {
                    try {
                        BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                        long lm = attr.lastModifiedTime().toMillis();
                        if (lm < cutoff) {
                            long sz = Files.size(p);
                            Files.deleteIfExists(p);
                            deletedBytes.addAndGet(sz);
                            deletedFiles.incrementAndGet();
                        }
                    } catch (IOException e) {
                        log.debug("[VideoAnalysisCleanup] 跳过文件 {}: {}", p, e.getMessage());
                    }
                });
            } catch (IOException e) {
                log.warn("[VideoAnalysisCleanup] 遍历失败 {}: {}", dir, e.getMessage());
            }
        }
        if (deletedFiles.get() > 0) {
            log.info("[VideoAnalysisCleanup] 删除 {} 个过期临时文件，约 {} 字节（早于 {} 小时）",
                    deletedFiles.get(), deletedBytes.get(), maxAgeHours);
        }
    }
}
