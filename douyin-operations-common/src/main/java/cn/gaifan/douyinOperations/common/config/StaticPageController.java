package cn.gaifan.douyinOperations.common.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * 显式提供登录页等静态页面，避免 Nginx/部署未配置静态根路径时 404。
 * 当请求能到达 Spring Boot 时，GET /pages/auth/login.html 会从此返回。
 */
@Controller
public class StaticPageController {

    private static final String LOGIN_RESOURCE = "static/pages/auth/login.html";

    @GetMapping("/pages/auth/login.html")
    public void loginPage(HttpServletResponse response) throws Exception {
        ClassPathResource resource = new ClassPathResource(LOGIN_RESOURCE);
        if (!resource.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        try (InputStream in = resource.getInputStream()) {
            StreamUtils.copy(in, response.getOutputStream());
        }
    }
}
