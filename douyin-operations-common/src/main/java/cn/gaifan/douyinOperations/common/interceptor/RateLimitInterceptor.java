package cn.gaifan.douyinOperations.common.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 速率限制拦截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimiter apiRateLimiter;

    @Autowired
    private RateLimiter loginRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {
        String uri = request.getRequestURI();
        RateLimiter limiter = uri.contains("/login") ? loginRateLimiter : apiRateLimiter;

        if (!limiter.acquirePermission()) {
            response.setStatus(429);  // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }
}
