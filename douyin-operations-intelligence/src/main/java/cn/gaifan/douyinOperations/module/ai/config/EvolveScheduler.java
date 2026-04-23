package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 知识进化引擎 - 定时调度（含分布式锁，多实例部署时仅单实例执行）。
 * 优先 Redis 锁；Redis 不可用时使用 DB 表 ai_evolve_scheduler_lock 作为备用。
 * 支持管理员在「系统配置」中动态修改 ai.evolve.interval-minutes（单位：分钟）。
 */
@Component
public class EvolveScheduler {

    private static final Logger log = LoggerFactory.getLogger(EvolveScheduler.class);
    private static final String LOCK_KEY = "ai:evolve:scheduler:lock";
    private static final String DB_LOCK_KEY = "scheduler";
    private static final long LOCK_TTL_HOURS = 2;

    @Value("${app.ai.evolve.enabled:true}")
    private boolean evolveEnabled;

    @Resource
    private EvolveEngineService evolveEngineService;

    @Resource
    private EvolveRoiService evolveRoiService;

    @Resource
    private AiRuntimeConfig aiRuntimeConfig;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private volatile long lastRunTime = 0;

    /** 每分钟检查一次，按 sys_config 中的 ai.evolve.interval-minutes 决定是否执行 */
    @Scheduled(fixedRate = 60000)
    public void scheduleEvolution() {
        if (!evolveEnabled) {
            log.debug("进化引擎已禁用，跳过定时调度");
            return;
        }
        int intervalMinutes = aiRuntimeConfig.getEvolveIntervalMinutes();
        long now = System.currentTimeMillis();
        if (lastRunTime > 0 && now - lastRunTime < (long) intervalMinutes * 60 * 1000) return;
        lastRunTime = now;

        if (!evolveRoiService.shouldRun()) {
            log.debug("进化引擎 ROI 降频中，跳过本轮（首次跳过时 EvolveRoiServiceImpl 会打 WARN 说明解除方式）");
            return;
        }

        String lockVal = String.valueOf(System.currentTimeMillis());
        String dbLockVal = null;
        boolean locked = false;
        boolean redisLockHeld = false;
        if (stringRedisTemplate != null) {
            try {
                locked = Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY, lockVal, LOCK_TTL_HOURS, TimeUnit.HOURS));
                if (!locked) {
                    log.debug("进化引擎未获取分布式锁，跳过（其他实例可能正在执行）");
                    return;
                }
                redisLockHeld = true;
            } catch (Exception e) {
                log.debug("Redis 不可用，进化引擎分布式锁改用 DB: {}", e.getMessage());
            }
        }
        if (!locked && jdbcTemplate != null) {
            dbLockVal = buildDbLockValue();
            try {
                jdbcTemplate.update("DELETE FROM ai_evolve_scheduler_lock WHERE lock_key = ? AND expire_at < NOW()", DB_LOCK_KEY);
                jdbcTemplate.update(
                        "INSERT INTO ai_evolve_scheduler_lock (lock_key, lock_value, expire_at) VALUES (?, ?, NOW() + INTERVAL '2 hours')",
                        DB_LOCK_KEY, dbLockVal);
                locked = true;
            } catch (DataIntegrityViolationException e) {
                log.debug("进化引擎未获取 DB 分布式锁，跳过（其他实例可能正在执行）");
                return;
            }
        }
        if (!locked) {
            return;
        }

        try {
            List<Long> kbIds = evolveEngineService.resolveEvolveKbIds();
            if (kbIds.size() <= 1) {
                for (Long kbId : kbIds) {
                    log.info("触发定时知识进化，kbId={}", kbId);
                    evolveEngineService.runEvolution(kbId, null);
                }
            } else {
                CompletableFuture<?>[] futures = kbIds.stream()
                        .map(kbId -> CompletableFuture.runAsync(() -> {
                            log.info("触发定时知识进化，kbId={}", kbId);
                            evolveEngineService.runEvolution(kbId, null);
                        }))
                        .toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(futures).join();
            }
        } finally {
            if (locked) {
                if (redisLockHeld && stringRedisTemplate != null) {
                    try {
                        if (lockVal.equals(stringRedisTemplate.opsForValue().get(LOCK_KEY))) {
                            stringRedisTemplate.delete(LOCK_KEY);
                        }
                    } catch (Exception ignored) {
                        // Redis 已不可用时忽略释放
                    }
                } else if (dbLockVal != null && jdbcTemplate != null) {
                    jdbcTemplate.update("DELETE FROM ai_evolve_scheduler_lock WHERE lock_key = ? AND lock_value = ?", DB_LOCK_KEY, dbLockVal);
                }
            }
        }
    }

    private static String buildDbLockValue() {
        try {
            return (InetAddress.getLocalHost().getHostName() + ":" + System.currentTimeMillis());
        } catch (Exception e) {
            return "node:" + System.currentTimeMillis();
        }
    }
}
