package cn.gaifan.douyinOperations.module.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 第三方 OAuth 配置（微信/QQ/抖音/火山），与 application.yml auth.oauth 绑定
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.oauth")
public class OAuthProviderProperties {

    /** 后端 callback 基础 URL，如 http://localhost:8091 */
    private String callbackBaseUrl = "http://localhost:8091";

    /** provider id -> 配置 */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private boolean enabled = false;
        private String appId = "";
        private String appSecret = "";
        private String authorizeUrl = "";
        private String tokenUrl = "";
        private String userInfoUrl = "";
        private String scope = "user_info";
    }
}
