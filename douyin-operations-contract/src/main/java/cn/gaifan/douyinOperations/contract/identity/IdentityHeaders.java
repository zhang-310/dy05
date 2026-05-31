package cn.gaifan.douyinOperations.contract.identity;

/**
 * 标准化的身份透传 HTTP Header 常量
 *
 * 微服务间调用时，网关/BFF 需要透传这些 header
 * 给下游服务，确保多租户上下文不丢失。
 */
public final class IdentityHeaders {

    private IdentityHeaders() {}

    /** 租户 ID */
    public static final String X_TENANT_ID = "X-Dy-Tenant-Id";

    /** 用户 ID */
    public static final String X_USER_ID = "X-Dy-User-Id";

    /** 角色编码 */
    public static final String X_ROLE_CODE = "X-Dy-Role-Code";

    /** 机构 ID */
    public static final String X_ORG_ID = "X-Dy-Org-Id";

    /** 渠道（WEB / APP / OPENAPI / MCP） */
    public static final String X_CHANNEL = "X-Dy-Channel";

    /** 分布式追踪 ID */
    public static final String X_TRACE_ID = "X-Dy-Trace-Id";

    /** 请求 ID */
    public static final String X_REQUEST_ID = "X-Dy-Request-Id";

    /** 服务间认证 Token（JWT） */
    public static final String X_SERVICE_AUTH = "X-Dy-Service-Auth";
}
