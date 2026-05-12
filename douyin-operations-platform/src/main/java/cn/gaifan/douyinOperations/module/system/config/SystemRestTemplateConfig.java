package cn.gaifan.douyinOperations.module.system.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;

/**
 * 提供带 API 调用日志拦截器的 RestTemplate，供 DouyinApiClient、AuthOAuthServiceImpl 等使用
 * P1-4: 配置连接池参数，防止连接泄漏
 */
@Configuration
public class SystemRestTemplateConfig {

    @Resource
    private ApiCallLogInterceptor apiCallLogInterceptor;

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // P1-4: 使用 HttpComponentsClientHttpRequestFactory 配置连接池
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);           // 连接超时 5 秒
        factory.setConnectionRequestTimeout(5000); // 从连接池获取连接超时 5 秒

        RestTemplate rt = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .requestFactory(() -> factory)
                .build();
        rt.setInterceptors(List.of(apiCallLogInterceptor));
        return rt;
    }
}
