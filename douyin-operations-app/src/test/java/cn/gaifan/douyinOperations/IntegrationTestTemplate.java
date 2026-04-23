/**
 * W-10 测试框架 - 集成测试样板（Karate + REST Assured）
 */

package cn.gaifan.douyinOperations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.restassured.http.ContentType;
import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import java.util.*;

/**
 * 集成测试样板
 *
 * 测试覆盖：
 * - 完整业务流程（创建 → 更新 → 查询 → 删除）
 * - 数据库事务一致性
 * - 错误处理和边界情况
 * - 并发场景
 */
@DisplayName("集成测试套件")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTestTemplate {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 示例 1：完整业务流程测试
     */
    @Test
    @DisplayName("应该完成完整的创建-更新-查询-删除流程")
    void testCompleteBusinessFlow() {
        // 1. 创建资源
        String createPayload = "{\"name\":\"测试项\",\"status\":\"active\"}";

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                "/api/v1/items",
                createPayload,
                String.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String itemId = extractIdFromResponse(createResponse.getBody());

        // 2. 更新资源
        String updatePayload = "{\"name\":\"更新的测试项\",\"status\":\"inactive\"}";

        ResponseEntity<String> updateResponse = restTemplate.postForEntity(
                "/api/v1/items/" + itemId,
                updatePayload,
                String.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. 查询资源
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                "/api/v1/items/" + itemId,
                String.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).contains("更新的测试项");

        // 4. 删除资源
        ResponseEntity<Void> deleteResponse = restTemplate.postForEntity(
                "/api/v1/items/" + itemId + "/delete",
                null,
                Void.class
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. 验证删除
        ResponseEntity<String> verifyResponse = restTemplate.getForEntity(
                "/api/v1/items/" + itemId,
                String.class
        );

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * 示例 2：使用 REST Assured 的 API 测试
     */
    @Test
    @DisplayName("使用 REST Assured 进行 API 测试")
    void testAPIWithRestAssured() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"keyword\":\"测试\"}")
        .when()
                .post("/api/v1/search")
        .then()
                .statusCode(200)
                .body("status", equalTo(200))
                .body("data.results", hasSize(greaterThan(0)))
                .body("data.total", greaterThan(0));
    }

    /**
     * 示例 3：错误处理测试
     */
    @Test
    @DisplayName("应该在参数无效时返回 400")
    void testErrorHandling() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")  // 空数据体
        .when()
                .post("/api/v1/items")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("message", containsString("参数校验失败"));
    }

    /**
     * 示例 4：并发测试
     */
    @Test
    @DisplayName("应该处理并发请求")
    void testConcurrentRequests() {
        int threadCount = 10;
        int requestsPerThread = 5;

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                            "/api/v1/items?page=0&rows=10",
                            String.class
                    );

                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                }
            });

            threads[i].start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 示例 5：数据库事务一致性测试
     */
    @Test
    @DisplayName("应该保证数据库事务一致性")
    void testTransactionConsistency() {
        // 创建两个相关的资源
        String parentPayload = "{\"name\":\"父资源\"}";
        ResponseEntity<String> parentResponse = restTemplate.postForEntity(
                "/api/v1/parents",
                parentPayload,
                String.class
        );

        String parentId = extractIdFromResponse(parentResponse.getBody());

        String childPayload = "{\"name\":\"子资源\",\"parentId\":" + parentId + "}";
        restTemplate.postForEntity(
                "/api/v1/children",
                childPayload,
                String.class
        );

        // 验证关系一致性
        ResponseEntity<String> getParentResponse = restTemplate.getForEntity(
                "/api/v1/parents/" + parentId + "/children",
                String.class
        );

        assertThat(getParentResponse.getBody()).contains("子资源");
    }

    /**
     * 示例 6：性能基准测试
     */
    @Test
    @DisplayName("API 响应时间应该在目标范围内")
    void testPerformanceBenchmark() {
        int iterations = 100;
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();

            restTemplate.getForEntity(
                    "/api/v1/items?page=0&rows=10",
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;
            responseTimes.add(duration);
        }

        // 计算统计数据
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95Time = getPercentile(responseTimes, 95);

        assertThat(avgTime).isLessThan(100);  // 平均 < 100ms
        assertThat(p95Time).isLessThan(150);  // P95 < 150ms
    }

    /**
     * 示例 7：WebSocket 实时推送测试
     */
    @Test
    @DisplayName("应该通过 WebSocket 接收实时更新")
    void testWebSocketConnection() {
        // 这个示例需要 WebSocket 客户端库（如 spring-websocket-tests）
        // 实际实现时需要完整的 WebSocket 客户端代码

        // 模拟 WebSocket 连接
        // WebSocketClient client = new StandardWebSocketClient();
        // ListenableFuture<WebSocketSession> future = client.doHandshake(...);
        // session.sendMessage(new TextMessage("test"));
    }

    /**
     * 辅助方法：从响应中提取 ID
     */
    private String extractIdFromResponse(String responseBody) {
        // 实际实现应该解析 JSON
        return "1";
    }

    /**
     * 辅助方法：计算百分位数
     */
    private long getPercentile(List<Long> values, int percentile) {
        values.sort(null);
        int index = (int) ((percentile / 100.0) * values.size());
        return values.get(Math.min(index, values.size() - 1));
    }
}
