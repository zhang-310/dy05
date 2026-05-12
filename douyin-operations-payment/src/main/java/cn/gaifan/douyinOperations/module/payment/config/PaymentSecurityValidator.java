package cn.gaifan.douyinOperations.module.payment.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * P1-5: 支付密钥配置验证器
 * 确保生产环境必须配置有效的支付密钥
 */
@Configuration
public class PaymentSecurityValidator {

    @Value("${payment.douyin.merchant-id:}")
    private String merchantId;

    @Value("${payment.douyin.merchant-secret:}")
    private String merchantSecret;

    @Value("${payment.douyin.app-id:}")
    private String appId;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void validate() {
        if ("prod".equals(activeProfile)) {
            // 生产环境必须配置支付密钥
            if (merchantId == null || merchantId.isBlank()) {
                throw new IllegalStateException("生产环境必须配置 DOUYIN_MERCHANT_ID");
            }

            if (merchantSecret == null || merchantSecret.length() < 32) {
                throw new IllegalStateException("生产环境必须配置 DOUYIN_MERCHANT_SECRET（至少 32 字符）");
            }

            if (appId == null || appId.isBlank()) {
                throw new IllegalStateException("生产环境必须配置 DOUYIN_APP_ID");
            }

            // 验证密钥不是测试值
            if (merchantSecret.contains("test") || merchantSecret.contains("dev")) {
                throw new IllegalStateException("生产环境不能使用测试密钥");
            }
        }
    }
}
