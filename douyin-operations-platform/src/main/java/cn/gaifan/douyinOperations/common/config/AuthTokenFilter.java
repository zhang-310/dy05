package cn.gaifan.douyinOperations.common.config;

import cn.gaifan.douyinOperations.common.constant.ApiAuthWhitelist;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.auth.AuthPermissionService;
import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 鉴权 Filter：/api/v1 白名单外校验 token，并校验 API 权限（角色-资源）
 * 未登录 2001、token 无效 2003、无权限 2002；通过后设置 request 属性 userId、roleCode
 */
@Component
@Order(2)
public class AuthTokenFilter implements Filter {

    private static final String PREFIX = "/api/v1";
    private static final String ATTR_USER_ID = "userId";
    private static final String ATTR_ROLE_CODE = "roleCode";
    private static final String ATTR_ORG_ID = "organizationId";

    @Resource
    private AuthTokenStore authTokenStore;
    @Resource
    private AuthPermissionService authPermissionService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        // 去掉 context-path，保证与 PREFIX 匹配（如 context-path=/app 时 URI 为 /app/api/v1/...）
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length()) : uri;
        if (!path.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        if (isWhitelist(path)) {
            chain.doFilter(request, response);
            return;
        }
        String token = extractToken(req);
        if (token == null || token.isEmpty()) {
            writeError(resp, ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
            return;
        }
        Long userId = authTokenStore.getUserId(token);
        if (userId == null) {
            writeError(resp, ErrorCode.TOKEN_INVALID, "token 无效");
            return;
        }
        String roleCode = authTokenStore.getRoleCode(token);
        req.setAttribute(ATTR_USER_ID, userId);
        req.setAttribute(ATTR_ROLE_CODE, roleCode);

        Long orgId = authTokenStore.getOrganizationId(token);
        if (orgId != null) {
            req.setAttribute(ATTR_ORG_ID, orgId);
        }

        String method = req.getMethod();
        if (!authPermissionService.hasPermission(userId, path, method)) {
            writeError(resp, ErrorCode.FORBIDDEN, "无权限访问");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isWhitelist(String path) {
        return ApiAuthWhitelist.contains(path);
    }

    private String extractToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7).trim();
        if (h != null && !h.isEmpty()) return h.trim();
        return req.getParameter("token");
    }

    private void writeError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");
        RESTResult<?> r = RESTResult.error(code, message);
        r.setTraceId(MDC.get("traceId"));
        resp.getWriter().write(objectMapper.writeValueAsString(r));
    }

    public static Long getUserId(HttpServletRequest request) {
        Object v = request.getAttribute(ATTR_USER_ID);
        return v instanceof Long ? (Long) v : null;
    }

    /** 当前请求用户角色编码（鉴权通过后由 Filter 设置） */
    public static String getRoleCode(HttpServletRequest request) {
        Object v = request.getAttribute(ATTR_ROLE_CODE);
        return v instanceof String ? (String) v : null;
    }

    /** 当前请求用户所属机构 ID（鉴权通过后由 Filter 设置，可能为 null） */
    public static Long getOrganizationId(HttpServletRequest request) {
        Object v = request.getAttribute(ATTR_ORG_ID);
        return v instanceof Long ? (Long) v : null;
    }
}
