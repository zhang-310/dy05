package cn.gaifan.douyinOperations.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0 API 契约冒烟：路径存在、统一 POST（除 GET OpenAPI）在无 Token 下返回 RESTResult 形态，避免前后端路径漂移导致 404。
 * 清单：{@code src/test/resources/p0-api-contract/p0-endpoints.json}，与 {@code docs/api/03-P0契约清单.md} 同步维护。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("P0 API 契约冒烟")
class P0ApiContractSmokeTest {

    private static final int UNAUTHORIZED = 2001;

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void registerInfra(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
        registry.add("auth.token-store", () -> "memory");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @ParameterizedTest(name = "{0}")
    @MethodSource("protectedCases")
    @DisplayName("受保护 POST：无 Token → status=2001")
    void protectedPostReturnsUnauthorized(String path, String bodyJson) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> res = restTemplate.exchange(
                path, HttpMethod.POST, new HttpEntity<>(bodyJson, headers), String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        JsonNode root = objectMapper.readTree(res.getBody());
        assertThat(root.has("status")).isTrue();
        assertThat(root.get("status").asInt()).isEqualTo(UNAUTHORIZED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("whitelistCases")
    @DisplayName("白名单 POST：非 5xx")
    void whitelistPostDoesNotServerError(String path, String bodyJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> res = restTemplate.exchange(
                path, HttpMethod.POST, new HttpEntity<>(bodyJson, headers), String.class);
        assertThat(res.getStatusCode().is5xxServerError()).isFalse();
        assertThat(res.getBody()).isNotBlank();
    }

    @Test
    @DisplayName("OpenAPI 文档可 GET")
    void openApiDocsAvailable() {
        ResponseEntity<String> res = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).contains("openapi");
    }

    private static Stream<Arguments> protectedCases() throws Exception {
        return loadPostCases("protectedPost");
    }

    private static Stream<Arguments> whitelistCases() throws Exception {
        return loadPostCases("whitelistPost");
    }

    private static Stream<Arguments> loadPostCases(String arrayField) throws Exception {
        ObjectMapper om = new ObjectMapper();
        try (InputStream in = P0ApiContractSmokeTest.class.getResourceAsStream("/p0-api-contract/p0-endpoints.json")) {
            assertThat(in).as("classpath p0-endpoints.json").isNotNull();
            JsonNode root = om.readTree(in);
            JsonNode arr = root.get(arrayField);
            List<Arguments> out = new ArrayList<>();
            for (JsonNode n : arr) {
                out.add(Arguments.of(n.get("path").asText(), om.writeValueAsString(n.get("body"))));
            }
            return out.stream();
        }
    }
}
