package cn.gaifan.douyinOperations.module.platform.payment;

import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentCreditGrantPort douyin-ops 履约：grantOnPayment reason 含 douyin-ops 时写 delivery 台账。
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PaymentCreditGrantAdapter douyin-ops delivery PG")
class PaymentCreditGrantAdapterDouyinOpsDeliveryTest {

    @Container
    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbcTemplate;
    private PaymentCreditGrantAdapter adapter;

    @BeforeAll
    void migrateAndWire() {
        postgres.start();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");
        jdbcTemplate = new JdbcTemplate(dataSource);
        adapter = new PaymentCreditGrantAdapter(jdbcTemplate);
        ReflectionTestUtils.setField(adapter, "deliveryLedgerService", new DeliveryLedgerService(jdbcTemplate));
    }

    @Test
    @DisplayName("grantOnPayment douyin-ops 写入 delivery 台账")
    void grantOnPayment_douyinOps_writesDeliveryLedger() {
        String tenantId = "org-payment-delivery";
        String traceId = "payment-douyin-ops-e2e-01";
        adapter.grantOnPayment(tenantId, "user-1", new BigDecimal("100"), traceId, "douyin-ops package purchase");

        Integer deliveryCount = jdbcTemplate.queryForObject(
                """
                        select count(*) from gf_douyin_ops_delivery_workspace_ledger
                        where tenant_id = ? and trace_id = ? and status = 'PAID'
                        """,
                Integer.class,
                tenantId,
                traceId
        );
        assertThat(deliveryCount).isNotNull().isGreaterThan(0);
    }
}
