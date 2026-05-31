package cn.gaifan.douyinOperations.common.config;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Gaifan 联调：当 {@code app.gaifan.allow-header-identity=true} 且请求带 X-Tenant-Id 时，
 * 为 /api/v1/ai 等路径注入 {@link IdentityContext}（trace 来自 MDC / X-Trace-Id）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CommercialHeaderIdentityFilter implements Filter {

    private static final String HEADER_TENANT = "X-Tenant-Id";
    private static final String HEADER_USER = "X-User-Id";
    private static final String HEADER_TRACE = "X-Trace-Id";

    @Value("${app.gaifan.allow-header-identity:false}")
    private boolean allowHeaderIdentity;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!allowHeaderIdentity) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        String tenantId = req.getHeader(HEADER_TENANT);
        if (tenantId == null || tenantId.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        String gfUserId = req.getHeader(HEADER_USER);
        String traceId = req.getHeader(HEADER_TRACE);
        if (traceId == null || traceId.isBlank()) {
            traceId = MDC.get("traceId");
        }
        String requestId = MDC.get("requestId");
        IdentityContext ctx = IdentityContext.commercialHeader(
                tenantId.trim(),
                gfUserId,
                detectChannel(req),
                traceId,
                requestId
        );
        RequestIdentityHolder.set(ctx);
        try {
            chain.doFilter(request, response);
        } finally {
            RequestIdentityHolder.clear();
        }
    }

    private static String detectChannel(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri.contains("/mcp")) {
            return "MCP";
        }
        if (uri.contains("/openapi")) {
            return "OPENAPI";
        }
        if (uri.contains("/ai")) {
            return "AI";
        }
        return "WEB";
    }
}
