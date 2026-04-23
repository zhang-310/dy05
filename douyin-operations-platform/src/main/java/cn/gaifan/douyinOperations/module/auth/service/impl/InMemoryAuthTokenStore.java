package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Token 存储（单机有效；集群需改为 Redis 实现）
 * Token 默认 24 小时过期，每 30 分钟清理一次过期条目
 */
@Component
@ConditionalOnProperty(name = "auth.token-store", havingValue = "memory")
public class InMemoryAuthTokenStore implements AuthTokenStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryAuthTokenStore.class);
    private static final long EXPIRE_MS = 24 * 60 * 60 * 1000L;

    private static class TokenInfo {
        final Long userId;
        final String roleCode;
        final Long organizationId;
        final long expireAt;

        TokenInfo(Long userId, String roleCode, Long organizationId, long expireAt) {
            this.userId = userId;
            this.roleCode = roleCode;
            this.organizationId = organizationId;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, TokenInfo> store = new ConcurrentHashMap<>();

    @Override
    public String createToken(Long userId, String roleCode) {
        return createToken(userId, roleCode, null);
    }

    @Override
    public String createToken(Long userId, String roleCode, Long organizationId) {
        String token = "tk_" + UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + EXPIRE_MS;
        store.put(token, new TokenInfo(userId, roleCode, organizationId, expireAt));
        return token;
    }

    @Override
    public Long getUserId(String token) {
        if (token == null || token.isEmpty()) return null;
        TokenInfo info = store.get(token);
        if (info == null || System.currentTimeMillis() > info.expireAt) return null;
        return info.userId;
    }

    @Override
    public String getRoleCode(String token) {
        if (token == null || token.isEmpty()) return null;
        TokenInfo info = store.get(token);
        if (info == null || System.currentTimeMillis() > info.expireAt) return null;
        return info.roleCode;
    }

    @Override
    public Long getOrganizationId(String token) {
        if (token == null || token.isEmpty()) return null;
        TokenInfo info = store.get(token);
        if (info == null || System.currentTimeMillis() > info.expireAt) return null;
        return info.organizationId;
    }

    @Override
    public void removeToken(String token) {
        if (token != null) store.remove(token);
    }

    @Override
    public boolean isValid(String token) {
        return getUserId(token) != null;
    }

    /**
     * 定时清理过期 Token，防止 ConcurrentHashMap 内存泄漏。
     * 每 30 分钟执行一次。
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        int removed = 0;
        Iterator<Map.Entry<String, TokenInfo>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (now > it.next().getValue().expireAt) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned {} expired tokens, {} active tokens remaining", removed, store.size());
        }
    }
}
