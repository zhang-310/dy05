package cn.gaifan.douyinOperations.module.payment.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentGatewayStartupValidatorTest {

    @Test
    void afterPropertiesSet_shouldAllowMockOutsideProd() {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();
        properties.setGatewayMode("mock");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        PaymentGatewayStartupValidator validator = new PaymentGatewayStartupValidator(properties, environment);

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void afterPropertiesSet_shouldRejectMockInProd() {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();
        properties.setGatewayMode("mock");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        PaymentGatewayStartupValidator validator = new PaymentGatewayStartupValidator(properties, environment);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("生产环境禁止使用 mock 支付网关");
    }

    @Test
    void afterPropertiesSet_shouldRejectDouyinModeWithoutCredentialsInProd() {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();
        properties.setGatewayMode("douyin");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        environment.setProperty("DOUYIN_MERCHANT_ID", "merchant-1");
        environment.setProperty("DOUYIN_MERCHANT_SECRET", "secret-1");

        PaymentGatewayStartupValidator validator = new PaymentGatewayStartupValidator(properties, environment);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DOUYIN_MERCHANT_ID/DOUYIN_MERCHANT_SECRET/DOUYIN_APP_ID");
    }

    @Test
    void afterPropertiesSet_shouldAllowDouyinModeWithCredentialsInProd() {
        PaymentGatewayProperties properties = new PaymentGatewayProperties();
        properties.setGatewayMode("douyin");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("DOUYIN_MERCHANT_ID", "merchant-1");
        environment.setProperty("DOUYIN_MERCHANT_SECRET", "secret-1");
        environment.setProperty("DOUYIN_APP_ID", "app-1");

        PaymentGatewayStartupValidator validator = new PaymentGatewayStartupValidator(properties, environment);

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }
}
