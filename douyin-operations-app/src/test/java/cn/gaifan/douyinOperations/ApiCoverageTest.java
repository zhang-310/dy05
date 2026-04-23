package cn.gaifan.douyinOperations;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 后端接口冒烟覆盖：对 /api/v1/** 下所有 POST/GET 端点各发送一次请求。
 * 任意响应（2xx/4xx/5xx）均视为端点已被调用；不替代业务断言。
 * <p>对外「100%」口径见 {@code docs/quality/00-对外验收口径与三层100%.md}；本类默认禁用，启用前需 test profile/DB/鉴权策略。
 */
@DisplayName("API 接口覆盖测试")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Requires full ApplicationContext; enable when test profile/DB is configured")
class ApiCoverageTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private static final String API_PREFIX = "/api/v1";
    private static final Pattern PATH_VAR = Pattern.compile("\\{[^}]+}");

    @Test
    @DisplayName("所有 /api/v1 端点至少被请求一次")
    void allApiEndpointsAreInvoked() {
        List<String> endpoints = collectApiEndpoints();
        assertThat(endpoints).isNotEmpty();

        List<String> failed = new ArrayList<>();
        for (String path : endpoints) {
            try {
                invokeEndpoint(path);
            } catch (Exception e) {
                failed.add(path + " -> " + e.getMessage());
            }
        }

        assertThat(failed)
                .as("以下端点请求异常，请检查或在排除列表中忽略: " + String.join("; ", failed))
                .isEmpty();
    }

    private List<String> collectApiEndpoints() {
        List<String> out = new ArrayList<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            var pathCond = info.getPathPatternsCondition();
            if (pathCond == null) continue;
            Set<PathPattern> patterns = pathCond.getPatterns();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                methods = Set.of(RequestMethod.GET, RequestMethod.POST);
            }
            for (PathPattern pp : patterns) {
                String pattern = pp.getPatternString();
                if (!pattern.startsWith(API_PREFIX)) continue;
                String path = PATH_VAR.matcher(pattern).replaceAll("1");
                for (RequestMethod method : methods) {
                    if (method == RequestMethod.GET || method == RequestMethod.POST) {
                        out.add(method.name() + " " + path);
                    }
                }
            }
        }
        return out.stream().distinct().sorted().toList();
    }

    private void invokeEndpoint(String methodAndPath) {
        String[] parts = methodAndPath.split(" ", 2);
        String method = parts[0];
        String path = parts[1];
        String url = "http://localhost:" + port + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response;
        if ("GET".equals(method)) {
            response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } else {
            response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>("{}", headers), String.class);
        }

        assertThat(response.getStatusCode()).isNotNull();
    }
}
