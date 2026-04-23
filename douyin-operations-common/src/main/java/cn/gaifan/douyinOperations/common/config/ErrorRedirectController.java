package cn.gaifan.douyinOperations.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 404 等错误时由 ErrorPageConfig 转发到此路径，重定向到登录页
 */
@Controller
public class ErrorRedirectController {

    private static final String LOGIN_PAGE = "/auth/login.html";

    @GetMapping("/error/404")
    public void redirect404(HttpServletResponse response) throws IOException {
        response.sendRedirect(LOGIN_PAGE);
    }
}
