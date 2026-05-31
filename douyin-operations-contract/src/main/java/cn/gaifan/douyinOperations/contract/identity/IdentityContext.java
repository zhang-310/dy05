package cn.gaifan.douyinOperations.contract.identity;

/**
 * 多租户身份上下文 — 纯数据 Record
 *
 * 每个请求经 UnifiedIdentityFilter 解析后生成此上下文，
 * 通过 RequestIdentityHolder 在当前线程传播。
 *
 * source 说明：
 * - USER_TOKEN: 标准 Bearer JWT 登录
 * - API_KEY:   OpenAPI / MCP 调用（X-Gaifan-Api-Key）
 * - AGENT:     企业 Agent 调用（X-Gaifan-Agent-Token）
 * - ANONYMOUS: 未认证请求（仅 whitelist 路径）
 */
public record IdentityContext(
        String source,
        boolean authenticated,
        String tenantId,
        Long userId,
        String roleCode,
        Long organizationId,
        String channel,
        String traceId,
        String requestId,
        /** Gaifan gf_* 表使用的 varchar userId（API Key / 联调 Header 场景） */
        String gfUserId
) {
    public static IdentityContext anonymous() {
        return new IdentityContext("ANONYMOUS", false, "default", null, null, null, "WEB", null, null, null);
    }

    public static IdentityContext authenticated(String tenantId, Long userId, String roleCode,
                                                Long organizationId, String channel,
                                                String traceId, String requestId) {
        return new IdentityContext("USER_TOKEN", true, tenantId, userId, roleCode,
                organizationId, channel, traceId, requestId, null);
    }

    /** E2E / OpenAPI 联调：X-Tenant-Id + X-User-Id 请求头（需 app.gaifan.allow-header-identity=true） */
    public static IdentityContext commercialHeader(String tenantId, String gfUserId, String channel,
                                                   String traceId, String requestId) {
        return new IdentityContext("COMMERCIAL_HEADER", true, tenantId, null, null, null,
                channel != null ? channel : "WEB", traceId, requestId,
                gfUserId != null && !gfUserId.isBlank() ? gfUserId : "demo-user");
    }
}
