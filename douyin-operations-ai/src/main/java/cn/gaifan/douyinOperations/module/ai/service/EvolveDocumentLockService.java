package cn.gaifan.douyinOperations.module.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;

/**
 * 进化引擎文档级分布式锁服务
 * 防止多实例/多线程并发进化同一文档。
 * 优先 Redis；Redis 不可用时回退到 DB 表 ai_evolve_document_lock。
 */
@Service
public class EvolveDocumentLockService {

    private static final Logger log = LoggerFactory.getLogger(EvolveDocumentLockService.class);
    private static final String LOCK_PREFIX = "ai:evolve:doc:lock:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * 尝试获取文档级锁
     * @param docId 文档 ID
     * @return 锁标识（非 null 表示获取成功），释放时需传回
     */
    public String tryLock(Long docId) {
        if (docId == null) return null;
        String lockVal = buildLockValue();

        // 优先 Redis
        if (stringRedisTemplate != null) {
            try {
                Boolean acquired = stringRedisTemplate.opsForValue()
                        .setIfAbsent(LOCK_PREFIX + docId, lockVal, LOCK_TTL);
                if (Boolean.TRUE.equals(acquired)) {
                    log.debug("文档级锁获取成功(Redis): docId={}", docId);
                    return lockVal;
                }
                log.debug("文档级锁已被占用(Redis): docId={}", docId);
                return null;
            } catch (Exception e) {
                log.warn("Redis 文档级锁获取异常，回退到 DB: {}", e.getMessage());
            }
        }

        // DB 回退
        if (jdbcTemplate != null) {
            try {
                // 清理过期锁
                jdbcTemplate.update("DELETE FROM ai_evolve_document_lock WHERE expires_at < NOW()");
                jdbcTemplate.update(
                        "INSERT INTO ai_evolve_document_lock (doc_id, locked_by, locked_at, expires_at) VALUES (?, ?, NOW(), NOW() + INTERVAL '30 minutes')",
                        docId, lockVal);
                log.debug("文档级锁获取成功(DB): docId={}", docId);
                return lockVal;
            } catch (Exception e) {
                log.debug("文档级锁已被占用(DB): docId={}", docId);
                return null;
            }
        }

        // 无锁基础设施，允许执行（单实例场景）
        log.warn("无 Redis/DB 可用，跳过文档级锁: docId={}", docId);
        return lockVal;
    }

    /**
     * 释放文档级锁
     * @param docId 文档 ID
     * @param lockVal tryLock 返回的锁标识
     */
    public void unlock(Long docId, String lockVal) {
        if (docId == null || lockVal == null) return;

        if (stringRedisTemplate != null) {
            try {
                String current = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + docId);
                if (lockVal.equals(current)) {
                    stringRedisTemplate.delete(LOCK_PREFIX + docId);
                    log.debug("文档级锁释放成功(Redis): docId={}", docId);
                }
                return;
            } catch (Exception e) {
                log.warn("Redis 文档级锁释放异常: {}", e.getMessage());
            }
        }

        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.update("DELETE FROM ai_evolve_document_lock WHERE doc_id = ? AND locked_by = ?", docId, lockVal);
                log.debug("文档级锁释放成功(DB): docId={}", docId);
            } catch (Exception e) {
                log.warn("DB 文档级锁释放异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 检查文档是否被锁定
     */
    public boolean isLocked(Long docId) {
        if (docId == null) return false;

        if (stringRedisTemplate != null) {
            try {
                return Boolean.TRUE.equals(stringRedisTemplate.hasKey(LOCK_PREFIX + docId));
            } catch (Exception e) {
                log.warn("Redis 检查文档锁异常: {}", e.getMessage());
            }
        }

        if (jdbcTemplate != null) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM ai_evolve_document_lock WHERE doc_id = ? AND expires_at > NOW()",
                        Integer.class, docId);
                return count != null && count > 0;
            } catch (Exception e) {
                log.warn("DB 检查文档锁异常: {}", e.getMessage());
            }
        }
        return false;
    }

    private static String buildLockValue() {
        try {
            return InetAddress.getLocalHost().getHostName() + ":" + Thread.currentThread().getName() + ":" + System.currentTimeMillis();
        } catch (Exception e) {
            return "node:" + Thread.currentThread().getName() + ":" + System.currentTimeMillis();
        }
    }
}
