package cn.gaifan.douyinOperations.common.config;

/**
 * 当前请求租户上下文（ThreadLocal）
 *
 * 用于多租户隔离：所有 DB 查询需按 tenantId 过滤。
 * 在 UnifiedIdentityFilter 中设置，请求完成后自动清除。
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        TENANT_ID.set(tenantId != null ? tenantId : "default");
    }

    public static String current() {
        String t = TENANT_ID.get();
        return t != null ? t : "default";
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
