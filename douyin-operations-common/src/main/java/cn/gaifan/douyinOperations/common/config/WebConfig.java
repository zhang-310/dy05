package cn.gaifan.douyinOperations.common.config;

import cn.gaifan.douyinOperations.common.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web 配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Value("${app.ai.image.local-upload-dir:uploads/images}")
    private String localImageUploadDir;

    @Value("${app.ai.image.public-url-prefix:/uploads/images}")
    private String imagePublicUrlPrefix;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/captcha",
                "/api/v1/auth/refresh-token"
            );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = imagePublicUrlPrefix == null || imagePublicUrlPrefix.isBlank()
                ? "/uploads/images"
                : imagePublicUrlPrefix.trim().replaceAll("/$", "");
        String location = Path.of(localImageUploadDir == null || localImageUploadDir.isBlank()
                        ? "uploads/images"
                        : localImageUploadDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(location);
    }
}
