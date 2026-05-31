package cn.gaifan.douyinOperations.module.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 支付网关启动校验：生产环境禁止 mock 支付，真实网关缺凭证时 fail-fast。
 */
@Component
@RequiredArgsConstructor
public class PaymentGatewayStartupValidator implements InitializingBean {

    private final PaymentGatewayProperties properties;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        boolean prod = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
        if (!prod) {
            return;
        }

        String mode = properties.getGatewayMode();
        if (!StringUtils.hasText(mode) || "mock".equalsIgnoreCase(mode)) {
            throw new IllegalStateException("生产环境禁止使用 mock 支付网关，请设置 PAYMENT_GATEWAY_MODE=douyin");
        }
        if ("douyin".equalsIgnoreCase(mode) && (!hasEnv("DOUYIN_MERCHANT_ID")
                || !hasEnv("DOUYIN_MERCHANT_SECRET")
                || !hasEnv("DOUYIN_APP_ID"))) {
            throw new IllegalStateException("生产环境启用抖音支付时必须配置 DOUYIN_MERCHANT_ID/DOUYIN_MERCHANT_SECRET/DOUYIN_APP_ID");
        }
    }

    private boolean hasEnv(String key) {
        return StringUtils.hasText(environment.getProperty(key));
    }
}
