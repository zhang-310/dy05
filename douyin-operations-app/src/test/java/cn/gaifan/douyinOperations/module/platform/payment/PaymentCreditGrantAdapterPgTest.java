package cn.gaifan.douyinOperations.module.platform.payment;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentCreditGrantPort 真实 PG：Flyway V215+ 后 grantOnPayment 写入 gf_credit_ledger。
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PaymentCreditGrantAdapter PG 集成")
class PaymentCreditGrantAdapterPgTest {

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
    }

    @Test
    @DisplayName("grantOnPayment 写入 payment.grant 台账")
    void grantOnPayment_writesCreditLedger() {
        String tenantId = "org-e2e-grant";
        adapter.grantOnPayment(tenantId, "user-1", new BigDecimal("50"), "payment-order-e2e-99", "PG test grant");

        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*) from gf_credit_ledger
                        where tenant_id = ? and feature_code = 'payment.grant' and trace_id = 'payment-order-e2e-99'
                        """,
                Integer.class,
                tenantId
        );
        assertThat(count).isNotNull().isGreaterThan(0);

        BigDecimal available = jdbcTemplate.queryForObject(
                "select available_credits from gf_credit_account where tenant_id = ?",
                BigDecimal.class,
                tenantId
        );
        assertThat(available).isGreaterThanOrEqualTo(new BigDecimal("50"));
    }
}
