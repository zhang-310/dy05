package cn.gaifan.douyinOperations.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SSE 流式响应过滤器：对 chat-stream、chat-sse、*-sse 等 SSE 端点禁用响应缓冲，确保数据立即推送到客户端。
 * Tomcat 默认 8KB 缓冲会导致流式数据被缓存，直到对话完毕才一次性返回。
 */
@Component
@Order(0)
public class SseStreamingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && isSseStreamingPath(uri)) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("X-Accel-Buffering", "no");
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isSseStreamingPath(String uri) {
        return uri != null && (uri.contains("chat-stream") || uri.contains("chat-sse") || uri.contains("-sse"));
    }
}
