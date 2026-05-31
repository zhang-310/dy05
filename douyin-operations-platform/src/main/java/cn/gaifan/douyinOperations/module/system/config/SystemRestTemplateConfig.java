package cn.gaifan.douyinOperations.module.system.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

import jakarta.annotation.Resource;
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
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate rt = builder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(factory))
                .build();
        rt.setInterceptors(List.of(apiCallLogInterceptor));
        return rt;
    }
}
