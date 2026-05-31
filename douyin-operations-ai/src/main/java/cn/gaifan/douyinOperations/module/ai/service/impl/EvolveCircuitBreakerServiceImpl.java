package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveCircuitBreakerService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class EvolveCircuitBreakerServiceImpl implements EvolveCircuitBreakerService {

    private static final String BREAKER_KEY = "ai:evolve:circuit:llm_balance";
    private static final String DEFAULT_REASON = "LLM account balance is insufficient; evolution is paused to avoid repeated failed tasks.";

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AiEvolveTaskRepository taskRepository;

    @Value("${app.ai.evolve.llm-balance-circuit.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.evolve.llm-balance-circuit.ttl-hours:12}")
    private long ttlHours;

    @Override
    public boolean isOpen() {
        if (!enabled) {
            return false;
        }
        if (isRedisOpen()) {
            return true;
        }
        return taskRepository.findFirstByOrderByUpdateTimeDesc()
                .map(t -> "failed".equals(t.getStatus()) && isBalanceError(t.getErrorMessage()) && withinRecentWindow(t.getUpdateTime()))
                .orElse(false);
    }

    @Override
    public String currentReason() {
        String redisReason = readRedisReason();
        if (redisReason != null && !redisReason.isBlank()) {
            return redisReason;
        }
        return taskRepository.findFirstByOrderByUpdateTimeDesc()
                .filter(t -> "failed".equals(t.getStatus()))
                .map(AiEvolveTask::getErrorMessage)
                .filter(this::isBalanceError)
                .orElse(DEFAULT_REASON);
    }

    @Override
    public void recordFailure(String errorMessage) {
        if (!enabled || !isBalanceError(errorMessage) || stringRedisTemplate == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(BREAKER_KEY, truncate(errorMessage, 500), Math.max(1, ttlHours), TimeUnit.HOURS);
        } catch (Exception ignored) {
            // Redis is best-effort only. The DB fallback in isOpen still protects after the failed task is saved.
        }
    }

    @Override
    public Map<String, Object> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        boolean open = isOpen();
        data.put("open", open);
        data.put("reason", open ? currentReason() : "");
        data.put("redisKey", BREAKER_KEY);
        data.put("ttlHours", ttlHours);
        return data;
    }

    private boolean isRedisOpen() {
        String reason = readRedisReason();
        return reason != null && !reason.isBlank();
    }

    private String readRedisReason() {
        if (!enabled || stringRedisTemplate == null) {
            return null;
        }
        try {
            return stringRedisTemplate.opsForValue().get(BREAKER_KEY);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isBalanceError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return false;
        }
        String s = errorMessage.toLowerCase(Locale.ROOT);
        return s.contains("insufficient balance")
                || s.contains("payment required")
                || s.contains("http 402")
                || s.contains("quota_exceeded")
                || s.contains("billing")
                || s.contains("余额不足")
                || s.contains("额度不足");
    }

    private boolean withinRecentWindow(Timestamp updateTime) {
        if (updateTime == null) {
            return true;
        }
        long ttlMs = Math.max(1, ttlHours) * 60L * 60L * 1000L;
        return System.currentTimeMillis() - updateTime.getTime() <= ttlMs;
    }

    private static String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
