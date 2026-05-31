package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 运行时配置：从 sys_config 读取，支持管理员后台动态修改。
 * 配置键见 sql/config/ai-runtime-config.sql
 */
@Component
public class AiRuntimeConfig {

    private static final Logger log = LoggerFactory.getLogger(AiRuntimeConfig.class);

    public static final String KEY_INDEX_QUEUE_INTERVAL_MS = "ai.index-queue.consumer.interval-ms";
    public static final String KEY_INDEX_QUEUE_BATCH_SIZE = "ai.index-queue.consumer.batch-size";
    public static final String KEY_EVOLVE_INTERVAL_MINUTES = "ai.evolve.interval-minutes";
    public static final String KEY_DUAL_WRITE_BATCH_SIZE = "ai.dual-write.compensation.batch-size";
    public static final String KEY_QUOTA_DAILY_MAX = "ai.quota.daily-max";
    public static final String KEY_RATE_LIMIT_API_PER_MINUTE = "app.rate-limit.api-per-minute";

    @Resource
    private ConfigService configService;

    @Value("${app.ai.index-queue.consumer.interval-ms:300000}")
    private long defaultIndexQueueIntervalMs;

    @Value("${app.ai.index-queue.consumer.batch-size:5}")
    private int defaultIndexQueueBatchSize;

    @Value("${app.ai.evolve.interval-minutes:60}")
    private int defaultEvolveIntervalMinutes;

    @Value("${app.ai.dual-write.compensation.batch-size:5}")
    private int defaultDualWriteBatchSize;

    /**
     * getRawValueByKey 已改为直读库；此处仍捕获异常以便 DB 异常时回退 yml 默认值。
     */
    private String safeGetRaw(String key) {
        if (configService == null) {
            return null;
        }
        try {
            return configService.getRawValueByKey(key);
        } catch (Exception e) {
            log.debug("读取 sys_config 失败，使用 yml 默认: key={}, {}", key, e.getMessage());
            return null;
        }
    }

    /** 从 sys_config 读取，无则用默认值 */
    public long getIndexQueueIntervalMs() {
        String v = safeGetRaw(KEY_INDEX_QUEUE_INTERVAL_MS);
        if (v != null && !v.isBlank()) {
            try {
                return Long.parseLong(v.trim());
            } catch (NumberFormatException ignored) {
                // 配置值格式错误，使用默认值
            }
        }
        return defaultIndexQueueIntervalMs;
    }

    /** 从 sys_config 读取，无则用默认值 */
    public int getIndexQueueBatchSize() {
        String v = safeGetRaw(KEY_INDEX_QUEUE_BATCH_SIZE);
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
                // 配置值格式错误，使用默认值
            }
        }
        return defaultIndexQueueBatchSize;
    }

    /** 从 sys_config 读取，无则用默认值。单位：分钟 */
    public int getEvolveIntervalMinutes() {
        String v = safeGetRaw(KEY_EVOLVE_INTERVAL_MINUTES);
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
                // 配置值格式错误，使用默认值
            }
        }
        return defaultEvolveIntervalMinutes;
    }

    /** 从 sys_config 读取，无则用默认值 */
    public int getDualWriteBatchSize() {
        String v = safeGetRaw(KEY_DUAL_WRITE_BATCH_SIZE);
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException ignored) {
                // 配置值格式错误，使用默认值
            }
        }
        return defaultDualWriteBatchSize;
    }
}
