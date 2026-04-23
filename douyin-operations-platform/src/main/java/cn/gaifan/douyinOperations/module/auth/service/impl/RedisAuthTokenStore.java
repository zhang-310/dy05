package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis 分布式 Token 存储（集群部署时使用）
 * Token 默认 24 小时过期，通过 Redis TTL 自动清理
 */
@Component
@ConditionalOnProperty(name = "auth.token-store", havingValue = "redis", matchIfMissing = true)
public class RedisAuthTokenStore implements AuthTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisAuthTokenStore.class);
    private static final String KEY_PREFIX = "auth:token:";
    private static final long EXPIRE_SECONDS = 24 * 60 * 60;

    private final StringRedisTemplate redisTemplate;

    public RedisAuthTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String createToken(Long userId, String roleCode) {
        return createToken(userId, roleCode, null);
    }

    @Override
    public String createToken(Long userId, String roleCode, Long organizationId) {
        String token = "tk_" + UUID.randomUUID().toString().replace("-", "");
        String key = KEY_PREFIX + token;

        // Store token data in Redis Hash
        redisTemplate.opsForHash().put(key, "userId", String.valueOf(userId));
        redisTemplate.opsForHash().put(key, "roleCode", roleCode);
        if (organizationId != null) {
            redisTemplate.opsForHash().put(key, "organizationId", String.valueOf(organizationId));
        }

        // Set TTL
        redisTemplate.expire(key, Duration.ofSeconds(EXPIRE_SECONDS));

        log.debug("Token created: userId={}, roleCode={}, organizationId={}", userId, roleCode, organizationId);
        return token;
    }

    @Override
    public Long getUserId(String token) {
        if (token == null || token.isEmpty()) return null;
        String key = KEY_PREFIX + token;

        Object val = redisTemplate.opsForHash().get(key, "userId");
        if (val == null) return null;

        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid userId in token: {}", token);
            return null;
        }
    }

    @Override
    public String getRoleCode(String token) {
        if (token == null || token.isEmpty()) return null;
        String key = KEY_PREFIX + token;

        Object val = redisTemplate.opsForHash().get(key, "roleCode");
        return val != null ? val.toString() : null;
    }

    @Override
    public Long getOrganizationId(String token) {
        if (token == null || token.isEmpty()) return null;
        String key = KEY_PREFIX + token;

        Object val = redisTemplate.opsForHash().get(key, "organizationId");
        if (val == null) return null;

        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            log.warn("Invalid organizationId in token: {}", token);
            return null;
        }
    }

    @Override
    public void removeToken(String token) {
        if (token != null && !token.isEmpty()) {
            String key = KEY_PREFIX + token;
            redisTemplate.delete(key);
            log.debug("Token removed: {}", token);
        }
    }

    @Override
    public boolean isValid(String token) {
        return getUserId(token) != null;
    }
}
