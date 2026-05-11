package cn.gaifan.douyinOperations.common.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("generalApiRateLimiter")
    private RateLimiter apiRateLimiter;

    @Autowired(required = false)
    @Qualifier("loginRateLimiter")
    private RateLimiter loginRateLimiter;

    @Autowired
    @Qualifier("oauthRateLimiter")
    private RateLimiter oauthRateLimiter;

    @Autowired(required = false)
    @Qualifier("videoSyncRateLimiter")
    private RateLimiter douyinSyncRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 选择合适的限流器
        RateLimiter limiter;
        if (uri.contains("/login") && loginRateLimiter != null) {
            limiter = loginRateLimiter;
        } else if (uri.contains("/oauth")) {
            limiter = oauthRateLimiter;
        } else if ((uri.contains("/douyin/video/sync") || uri.contains("/douyin/fans/sync")) && douyinSyncRateLimiter != null) {
            limiter = douyinSyncRateLimiter;
        } else {
            limiter = apiRateLimiter;
        }

        if (limiter != null && !limiter.acquirePermission()) {
            response.setStatus(429);  // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }
}
