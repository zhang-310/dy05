package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * P1 向量缓存预热：对高频主题预先向量化，减少冷启动延迟。
 * 在 ApplicationReadyEvent 后执行，确保上下文完全就绪，避免在 DevTools 重启等场景下使用已销毁的 Bean。
 */
@Component
@ConditionalOnProperty(name = "app.ai.warmup-enabled", havingValue = "true", matchIfMissing = false)
public class VectorCacheWarmup {

    private static final Logger log = LoggerFactory.getLogger(VectorCacheWarmup.class);

    @Autowired(required = false)
    private VectorService vectorService;

    @Value("${app.ai.warmup-topics:}")
    private String warmupTopics;

    @Value("${app.ai.warmup-delay-seconds:30}")
    private int warmupDelaySeconds;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleWarmup() {
        if (vectorService == null || warmupTopics == null || warmupTopics.isBlank()) {
            return;
        }
        List<String> topics = Arrays.stream(warmupTopics.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (topics.isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                Thread.sleep(warmupDelaySeconds * 1000L);
                runWarmup(topics);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("向量缓存预热延迟被中断");
            }
        }, "vector-cache-warmup").start();
    }

    private void runWarmup(List<String> topics) {
        log.info("P1 向量缓存预热开始，主题数: {}", topics.size());
        int ok = 0, fail = 0;
        for (String topic : topics) {
            try {
                vectorService.generateEmbedding(topic);
                ok++;
            } catch (Exception e) {
                fail++;
                log.warn("预热主题失败 [{}]: {}", topic, e.getMessage());
            }
        }
        log.info("P1 向量缓存预热完成，成功: {}, 失败: {}", ok, fail);
    }
}
