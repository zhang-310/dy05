package cn.gaifan.douyinOperations.module.log.config;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.util.IPUtils;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.log.service.OperationLogService;
import cn.gaifan.douyinOperations.module.log.util.BodyMaskUtil;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求完成后记录操作日志（仅 /api/v1 且已鉴权通过的请求；登录由事件单独记录）。
 * 使用 ContentCaching 包装器采集 request/response body，脱敏后写入 request_body/response_body（各最多 2000 字符）。
 */
@Component
@Order(3)
public class OperationLogFilter implements Filter {

    private static final String PREFIX = "/api/v1";
    private static final int BODY_MAX_LENGTH = 2000;

    @Resource
    private OperationLogService operationLogService;
    @Resource
    private AuthUserRepository authUserRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length()) : uri;

        if (!path.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(req);
        StatusCaptureResponseWrapper statusWrapper = new StatusCaptureResponseWrapper(resp);
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(statusWrapper);
        try {
            chain.doFilter(cachingRequest, cachingResponse);
        } finally {
            String requestBody = BodyMaskUtil.maskAndTruncate(
                    cachingRequest.getContentAsByteArray(), req.getContentType(), BODY_MAX_LENGTH);
            String responseBody = BodyMaskUtil.maskAndTruncate(
                    cachingResponse.getContentAsByteArray(), resp.getContentType(), BODY_MAX_LENGTH);
            try {
                cachingResponse.copyBodyToResponse();
            } catch (IOException ignored) {
                // 复制响应体失败不影响主流程
            }
            try {
                Long userId = AuthTokenFilter.getUserId(req);
                if (userId != null) {
                    String username = authUserRepository.findById(userId).map(AuthUser::getUsername).orElse(null);
                    String module = parseModule(path);
                    String action = parseAction(path);
                    String method = req.getMethod();
                    String ip = IPUtils.getRealIP(req);
                    String userAgent = req.getHeader("User-Agent");
                    int status = statusWrapper.getCapturedStatus();
                    int statusOk = (status >= 200 && status < 300) ? 1 : 0;
                    String traceId = MDC.get("traceId");
                    int durationMs = (int) (System.currentTimeMillis() - start);
                    operationLogService.save(userId, username, module, action, path, method, ip, userAgent,
                            durationMs, statusOk, null, traceId, requestBody, responseBody);
                }
            } catch (Exception ignored) {
                // 日志落库失败不影响响应
            }
        }
    }

    private static String parseModule(String path) {
        if (path == null) return "unknown";
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("v1".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return "api";
    }

    private static String parseAction(String path) {
        if (path == null) return "";
        int last = path.lastIndexOf('/');
        if (last >= 0 && last < path.length() - 1) {
            return path.substring(last + 1);
        }
        return "request";
    }
}
