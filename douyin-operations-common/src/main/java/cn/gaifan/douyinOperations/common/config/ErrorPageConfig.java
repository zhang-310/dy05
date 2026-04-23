package cn.gaifan.douyinOperations.common.config;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 全局错误页：404 跳转登录页（未找到资源时统一到登录页）
 */
@Component
public class ErrorPageConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private static final String REDIRECT_404_PATH = "/error/404";

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, REDIRECT_404_PATH));
    }
}
