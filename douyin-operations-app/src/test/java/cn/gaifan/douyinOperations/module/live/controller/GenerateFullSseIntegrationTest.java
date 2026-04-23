package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * generate-full-sse 端点连通性测试。
 *
 * 重要：本测试仅验证 200 + Content-Type，不验证 progress/slot_done/done 事件是否流式下发。
 * 若前端长时间无输出、仅最后一次性返回，本测试仍会通过，属已知局限。
 *
 * 流式是否正常必须手工验证：http://localhost:8080/test-sse.html 一键生成测试，
 * 应能持续看到「收到第 N 块」和 progress 事件，而非等 2 分钟后一次性显示。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Disabled("Requires full ApplicationContext; enable when test profile is configured")
@DisplayName("generate-full-sse 端点连通性（不验证流式内容）")
class GenerateFullSseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveAiService liveAiService;

    private String token;

    @BeforeEach
    void setUp() {
        token = loginAndGetToken();
    }

    private String loginAndGetToken() {
        byte[] bodyBytes = webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"username\":\"admin\",\"password\":\"admin123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();
        if (bodyBytes == null) throw new IllegalStateException("登录失败");
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = objectMapper.readValue(body, Map.class);
            Object data = json.get("data");
            if (data instanceof Map) {
                Object t = ((Map<?, ?>) data).get("token");
                if (t != null) return t.toString();
            }
            Object t = json.get("token");
            if (t != null) return t.toString();
        } catch (Exception e) {
            throw new IllegalStateException("解析 token 失败: " + body, e);
        }
        throw new IllegalStateException("登录响应无 token: " + body);
    }

    @Test
    @DisplayName("端点返回 200 且 Content-Type 为 text/event-stream（不验证事件是否流式下发）")
    void endpointReturnsOkAndCorrectContentType() throws Exception {
        when(liveAiService.generateFullWithProgress(any(LiveAiGenerateVO.class), any())).thenAnswer(inv -> {
            LiveAiService.FullGenerateProgressCallback cb = inv.getArgument(1);
            cb.onProgress(0, 1, "连接成功，准备生成");
            cb.onProgress(1, 1, "生成完成");
            LiveAiFullResultVO vo = new LiveAiFullResultVO();
            vo.setResults(List.of());
            vo.setConsumption(1);
            return vo;
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "text/event-stream");
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>("{\"sessionId\":1,\"style\":\"\",\"modelId\":1}", headers);

        var response = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/live/ai/generate-full-sse",
                org.springframework.http.HttpMethod.POST,
                entity,
                Void.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("Content-Type"))
                .as("Content-Type 应为 text/event-stream")
                .contains("text/event-stream");
    }
}
