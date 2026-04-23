package cn.gaifan.douyinOperations.common.util;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 乐观锁重试工具
 */
public class RetryableAction {
    private static final Logger log = LoggerFactory.getLogger(RetryableAction.class);
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 执行带乐观锁重试的操作
     */
    public static <T> T executeWithRetry(Supplier<T> action) {
        return executeWithRetry(action, DEFAULT_MAX_RETRIES);
    }

    public static <T> T executeWithRetry(Supplier<T> action, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockException e) {
                if (attempt == maxRetries) {
                    log.warn("乐观锁重试 {} 次后仍然失败", maxRetries);
                    throw e;
                }
                log.info("乐观锁冲突，第 {} 次重试", attempt);
            }
        }
        throw new IllegalStateException("不应该到达这里");
    }

    public static void executeWithRetry(Runnable action) {
        executeWithRetry(action, DEFAULT_MAX_RETRIES);
    }

    public static void executeWithRetry(Runnable action, int maxRetries) {
        executeWithRetry(() -> { action.run(); return null; }, maxRetries);
    }
}
