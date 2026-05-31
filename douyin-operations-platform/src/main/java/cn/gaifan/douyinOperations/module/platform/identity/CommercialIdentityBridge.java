package cn.gaifan.douyinOperations.module.platform.identity;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * dy05 BIGINT 用户/机构身份 → Gaifan gf_* varchar tenantId/userId 映射。
 */
public final class CommercialIdentityBridge {

    private CommercialIdentityBridge() {
    }

    public static String resolveTenantId(IdentityContext ctx) {
        if (ctx == null || !ctx.authenticated()) {
            return "default";
        }
        if (ctx.tenantId() != null && !ctx.tenantId().isBlank() && !"default".equals(ctx.tenantId())) {
            return ctx.tenantId();
        }
        if (ctx.organizationId() != null) {
            return "org-" + ctx.organizationId();
        }
        if (ctx.userId() != null) {
            return "user-" + ctx.userId();
        }
        return "default";
    }

    public static String resolveUserId(IdentityContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.gfUserId() != null && !ctx.gfUserId().isBlank()) {
            return ctx.gfUserId();
        }
        if (ctx.userId() == null) {
            return null;
        }
        return "user-" + ctx.userId();
    }

    public static String resolveTraceId(IdentityContext ctx) {
        if (ctx != null && ctx.traceId() != null && !ctx.traceId().isBlank()) {
            return ctx.traceId();
        }
        String mdc = MDC.get("traceId");
        if (mdc != null && !mdc.isBlank()) {
            return mdc;
        }
        return "trace-" + UUID.randomUUID().toString().replace("-", "");
    }
}
