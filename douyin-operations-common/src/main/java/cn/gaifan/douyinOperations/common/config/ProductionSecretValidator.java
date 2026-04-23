package cn.gaifan.douyinOperations.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 生产环境启动前校验关键密钥，避免空密钥或弱默认上线。
 * 开发/测试 profile 不加载。
 */
@Component
@Profile("prod")
public class ProductionSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecretValidator.class);

    @Value("${app.token.secret:}")
    private String tokenSecret;

    @Value("${tianapi.enabled:false}")
    private boolean tianapiEnabled;

    @Value("${tianapi.api-key:}")
    private String tianapiApiKey;

    @PostConstruct
    public void validate() {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException("生产环境必须配置 APP_TOKEN_SECRET（app.token.secret），且非空");
        }
        if (tokenSecret.length() < 32) {
            throw new IllegalStateException("APP_TOKEN_SECRET 长度建议至少 32 字符");
        }
        String weak = "dev-only-change-in-production";
        if (tokenSecret.contains(weak) || tokenSecret.equals("test-secret-key-for-testing-only")) {
            throw new IllegalStateException("APP_TOKEN_SECRET 不能使用开发/测试占位值");
        }
        if (tianapiEnabled && (tianapiApiKey == null || tianapiApiKey.isBlank())) {
            log.warn("tianapi.enabled=true 但未配置 TIANAPI_API_KEY，天行 API 调用将失败");
        }
    }
}
