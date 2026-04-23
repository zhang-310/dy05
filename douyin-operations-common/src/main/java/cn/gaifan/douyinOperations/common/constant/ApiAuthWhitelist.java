package cn.gaifan.douyinOperations.common.constant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 鉴权白名单路径，由 {@link cn.gaifan.douyinOperations.common.config.AuthTokenFilter} 复用，
 * 避免在过滤器中重复硬编码。新增免登录 API 时只改此处并同步文档。
 */
public final class ApiAuthWhitelist {

    private ApiAuthWhitelist() {}

    public static final String[] PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/captcha",
            "/api/v1/auth/sms/send",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/oauth/authorize",
            "/api/v1/auth/oauth/callback",
            "/api/v1/messaging/webhook/feishu",
            "/api/v1/messaging/webhook/wecom"
    };

    private static final Set<String> PATH_SET;

    static {
        HashSet<String> s = new HashSet<>();
        Collections.addAll(s, PATHS);
        PATH_SET = Collections.unmodifiableSet(s);
    }

    public static boolean contains(String path) {
        return path != null && PATH_SET.contains(path);
    }
}
