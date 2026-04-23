package cn.gaifan.douyinOperations.module.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 提供带 API 调用日志拦截器的 RestTemplate，供 DouyinApiClient、AuthOAuthServiceImpl 等使用
 */
@Configuration
public class SystemRestTemplateConfig {

    @Resource
    private ApiCallLogInterceptor apiCallLogInterceptor;

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.setInterceptors(List.of(apiCallLogInterceptor));
        return rt;
    }
}
