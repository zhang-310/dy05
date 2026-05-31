package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
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
 * 真实 PG：Flyway V215+ 后 consume 写入 gf_credit_ledger（不依赖完整 Spring Boot 启动）。
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("CreditLedgerService consume PG 集成")
class CreditLedgerConsumePgTest {

    @Container
    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbcTemplate;
    private CreditLedgerService creditLedgerService;

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
        creditLedgerService = new CreditLedgerService(jdbcTemplate);
    }

    @Test
    @DisplayName("consume 写入 gf_credit_ledger")
    void consume_writesCreditLedger() {
        Integer before = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where tenant_id = 'demo-tenant'",
                Integer.class
        );

        CreditConsumeResult result = creditLedgerService.consume(new CreditConsumeRequest(
                "demo-tenant",
                "demo-user",
                null,
                "video-insight",
                "video-insight.breakdown",
                "WEB",
                BigDecimal.ONE,
                "standard",
                "pg-e2e-consume-ledger",
                "CreditLedgerConsumePgTest"
        ));

        assertThat(result.allowed()).isTrue();

        Integer after = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where tenant_id = 'demo-tenant'",
                Integer.class
        );
        assertThat(after).isGreaterThan(before);

        Integer traceHits = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where trace_id = 'pg-e2e-consume-ledger'",
                Integer.class
        );
        assertThat(traceHits).isGreaterThan(0);
    }
}
