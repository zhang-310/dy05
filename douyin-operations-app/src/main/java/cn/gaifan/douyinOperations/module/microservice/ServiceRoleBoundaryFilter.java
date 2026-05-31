package cn.gaifan.douyinOperations.module.microservice;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务角色边界过滤器
 *
 * 根据当前服务角色过滤 API 请求，越界请求返回 404。
 * 参考 gaifan-ops ServiceRoleBoundaryFilter。
 *
 * 角色-路径映射：
 * platform → /api/v1/auth, /api/v1/system, /api/v1/config,
 *            /api/v1/payment, /api/v1/log, /api/v1/openapi
 * ai-mcp   → /api/v1/ai, /api/v1/agent, /api/v1/integration
 * content  → /api/v1/douyin, /api/v1/live, /api/v1/short-video,
 *            /api/v1/drama, /api/v1/script, /api/v1/copy,
 *            /api/v1/storage, /api/v1/benchmark, /api/v1/search,
 *            /api/v1/compliance, /api/v1/digital-human,
 *            /api/v1/photo-avatar, /api/v1/workflow
 */
@Component
@Order(1)
public class ServiceRoleBoundaryFilter implements Filter {

    private static final String PREFIX = "/api/v1";

    private static final Map<String, List<String>> ROLE_PREFIXES = Map.of(
            "platform", List.of(
                    "/api/v1/auth", "/api/v1/system", "/api/v1/config",
                    "/api/v1/payment", "/api/v1/log", "/api/v1/monitoring",
                    "/api/v1/openapi", "/api/v1/bff", "/api/v1/admin",
                    "/api/v1/org", "/api/v1/user",
                    "/api/v1/product", "/api/v1/governance", "/api/v1/billing",
                    "/api/credits", "/api/platform", "/api/governance",
                    "/api/public-site", "/api/docs-center", "/api/mcp", "/api/openapi", "/mcp"
            ),
            "ai-mcp", List.of(
                    "/api/v1/ai", "/api/v1/agent", "/api/v1/integration",
                    "/api/v1/knowledge", "/api/v1/mcp"
            ),
            "content", List.of(
                    "/api/v1/douyin", "/api/v1/live", "/api/v1/short-video",
                    "/api/v1/drama", "/api/v1/script", "/api/v1/copy",
                    "/api/v1/storage", "/api/v1/benchmark", "/api/v1/search",
                    "/api/v1/compliance", "/api/v1/video-insight",
                    "/api/v1/digital-human", "/api/v1/photo-avatar", "/api/v1/workflow"
            )
    );

    @Resource
    private ServiceRoleProperties serviceRoleProperties;

    @Resource
    private ObjectMapper objectMapper;

    // P1: API 限流计数器 (per tenant + path)
    private static final int DEFAULT_RATE_LIMIT = 600; // 600 req/min
    private static final int OPENAPI_RATE_LIMIT = 120;  // 120 req/min
    private final Map<String, AtomicInteger> rateCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> rateWindows = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // P1: API 限流检查（全角色生效）
        if (path.startsWith(PREFIX)) {
            String rateKey = serviceRoleProperties.getRole() + ":"
                    + (path.contains("/openapi") ? "OPENAPI" : path.substring(PREFIX.length(), Math.min(path.length(), PREFIX.length() + 20)));
            long now = System.currentTimeMillis() / 60000; // 分钟级窗口
            Long lastWindow = rateWindows.get(rateKey);
            if (lastWindow == null || lastWindow < now) {
                rateWindows.put(rateKey, now);
                rateCounters.computeIfAbsent(rateKey, k -> new AtomicInteger(0)).set(0);
            }
            int limit = path.contains("/openapi") ? OPENAPI_RATE_LIMIT : DEFAULT_RATE_LIMIT;
            int count = rateCounters.computeIfAbsent(rateKey, k -> new AtomicInteger(0)).incrementAndGet();
            if (count > limit) {
                resp.setStatus(429); // Too Many Requests
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"status\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
                return;
            }
        }

        if (serviceRoleProperties.isAllInOne()) {
            chain.doFilter(request, response);
            return;
        }

        String role = serviceRoleProperties.getRole();

        if (!path.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        List<String> allowed = ROLE_PREFIXES.get(role);
        if (allowed == null) {
            writeForbidden(resp, role, path);
            return;
        }

        boolean matched = allowed.stream().anyMatch(path::startsWith);
        if (!matched) {
            writeForbidden(resp, role, path);
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeForbidden(HttpServletResponse resp, String role, String path) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("application/json;charset=UTF-8");
        RESTResult<?> r = RESTResult.error(404,
                "当前服务角色[" + role + "]不承载该接口：" + path);
        r.setTraceId(MDC.get("traceId"));
        resp.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
