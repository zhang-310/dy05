package cn.gaifan.douyinOperations.module.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付网关模式：mock 用于联调；douyin 需配置商户凭证与回调 URL（见环境变量）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentGatewayProperties {

    /** mock | douyin */
    private String gatewayMode = "mock";

    private Douyin douyin = new Douyin();

    @Data
    public static class Douyin {
        private boolean enabled = false;
        /** 支付回调来源 IP 白名单，逗号分隔；空表示不做 IP 限制。 */
        private String callbackIpWhitelist = "";
    }
}
