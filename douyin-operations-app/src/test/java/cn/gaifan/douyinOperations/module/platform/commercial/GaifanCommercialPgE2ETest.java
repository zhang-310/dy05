package cn.gaifan.douyinOperations.module.platform.commercial;

import cn.gaifan.douyinOperations.contract.commerce.PaymentCreditGrantPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gaifan 商业化真实 PG：Flyway + consume + PaymentCreditGrantPort。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "gaifan-e2e"})
@Testcontainers(disabledWithoutDocker = true)
@Tag("pg-e2e")
@DisplayName("Gaifan 商业化 PG E2E")
class GaifanCommercialPgE2ETest {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void registerInfra(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
        registry.add("auth.token-store", () -> "memory");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private PaymentCreditGrantPort paymentCreditGrantPort;

    @Test
    @DisplayName("POST /api/credits/consume 写入 gf_credit_ledger")
    void consume_writesCreditLedger() throws Exception {
        Integer before = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where tenant_id = 'demo-tenant'",
                Integer.class
        );

        String body = objectMapper.writeValueAsString(Map.of(
                "tenantId", "demo-tenant",
                "userId", "demo-user",
                "productCode", "video-insight",
                "featureCode", "video-insight.breakdown",
                "channel", "WEB",
                "requestedAmount", 1,
                "traceId", "pg-e2e-consume-test",
                "reason", "GaifanCommercialPgE2ETest"
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> res = restTemplate.postForEntity(
                "/api/credits/consume",
                new HttpEntity<>(body, headers),
                String.class
        );
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode root = objectMapper.readTree(res.getBody());
        assertThat(root.path("status").asInt()).isZero();

        Integer after = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where tenant_id = 'demo-tenant'",
                Integer.class
        );
        assertThat(after).isGreaterThan(before);

        Integer traceHits = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where trace_id = 'pg-e2e-consume-test'",
                Integer.class
        );
        assertThat(traceHits).isGreaterThan(0);
    }

    @Test
    @DisplayName("PaymentCreditGrantPort 直测入账")
    void confirmPayment_grantsCredits() {
        assertThat(paymentCreditGrantPort).isNotNull();
        String tenantId = "org-pg-e2e-" + System.currentTimeMillis();
        paymentCreditGrantPort.grantOnPayment(
                tenantId,
                "user-pg-e2e",
                new BigDecimal("25"),
                "payment-order-pg-e2e",
                "GaifanCommercialPgE2ETest grant"
        );
        Integer grants = jdbcTemplate.queryForObject(
                """
                        select count(*) from gf_credit_ledger
                        where tenant_id = ? and feature_code = 'payment.grant'
                        """,
                Integer.class,
                tenantId
        );
        assertThat(grants).isGreaterThan(0);
    }
}
