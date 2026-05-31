package cn.gaifan.douyinOperations.common.config;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;

/**
 * 当前请求身份上下文持有者（ThreadLocal）
 *
 * 在 UnifiedIdentityFilter 中设置，请求完成后自动清除。
 * 供 Controller、Service、Async 任务统一获取当前用户/租户。
 *
 * 用法：
 *   IdentityContext ctx = RequestIdentityHolder.current();
 *   Long userId = ctx.userId();
 *
 * 向后兼容：现有的 AuthTokenFilter.getUserId(request) 仍可继续使用。
 */
public final class RequestIdentityHolder {

    private static final ThreadLocal<IdentityContext> CONTEXT = new ThreadLocal<>();

    private RequestIdentityHolder() {}

    public static void set(IdentityContext context) {
        CONTEXT.set(context);
    }

    public static IdentityContext current() {
        IdentityContext ctx = CONTEXT.get();
        return ctx != null ? ctx : IdentityContext.anonymous();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
