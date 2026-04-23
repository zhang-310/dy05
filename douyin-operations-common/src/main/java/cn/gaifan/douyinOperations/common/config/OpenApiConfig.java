package cn.gaifan.douyinOperations.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger 文档配置
 * 启动后访问 /swagger-ui.html 或 /v3/api-docs
 * RestTemplate 由 system 模块 SystemRestTemplateConfig 提供（带 API 调用日志拦截器）
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("抖音运营后台 API")
                        .version("1.0")
                        .description("统一返回 RESTResult（status/msg/data/traceId）；成功 status=200；权限 2001/2002/2003 见错误码表"));
    }
}
