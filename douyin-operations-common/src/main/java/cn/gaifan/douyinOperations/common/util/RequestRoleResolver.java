package cn.gaifan.douyinOperations.common.util;

import cn.gaifan.douyinOperations.common.constant.RoleCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从当前请求中提取角色编码的工具类。
 * 用于 Service 层需要判断当前用户角色（如 admin 绕过数据归属检查）的场景。
 */
public final class RequestRoleResolver {

    private RequestRoleResolver() {}

    /**
     * 从当前 HTTP 请求的 attribute 中获取 roleCode（由 AuthTokenFilter 设置）。
     * 如果当前线程不在 HTTP 请求上下文中，返回 null。
     */
    public static String getCurrentRoleCode() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        HttpServletRequest req = attrs.getRequest();
        Object rc = req.getAttribute("roleCode");
        return rc instanceof String ? (String) rc : null;
    }

    /**
     * 判断当前请求的用户是否为管理员角色。
     */
    public static boolean isAdmin() {
        return RoleCode.ADMIN.equals(getCurrentRoleCode());
    }
}
