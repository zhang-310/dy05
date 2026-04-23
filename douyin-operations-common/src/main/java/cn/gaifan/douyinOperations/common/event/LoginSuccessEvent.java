package cn.gaifan.douyinOperations.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 登录成功事件（用于记录操作日志，由 auth 发布、log 监听）
 */
public class LoginSuccessEvent extends ApplicationEvent {

    private final Long userId;
    private final String username;
    private final String requestUri;
    private final String requestMethod;
    private final String ip;
    private final String userAgent;

    public LoginSuccessEvent(Object source, Long userId, String username, String requestUri, String requestMethod,
                            String ip, String userAgent) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.requestUri = requestUri;
        this.requestMethod = requestMethod;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRequestUri() { return requestUri; }
    public String getRequestMethod() { return requestMethod; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
}
