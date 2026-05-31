package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 本地 dy-postgres（5433）consume E2E；需先执行 scripts/apply-gaifan-commercial-migrations.ps1。
 * 运行：{@code mvn -pl douyin-operations-app test -Dtest=CreditLedgerConsumeLocalPgIT -Dgaifan.pg.local=true}
 */
@DisplayName("CreditLedger consume 本地 PG IT")
class CreditLedgerConsumeLocalPgIT {

    private static final String JDBC_URL = System.getProperty(
            "gaifan.pg.url",
            "jdbc:postgresql://localhost:5433/douyin_operations"
    );
    private static final String JDBC_USER = System.getProperty("gaifan.pg.user", "postgres");
    private static final String JDBC_PASSWORD = System.getProperty("gaifan.pg.password", "postgresql");

    @Test
    @DisplayName("本地 PG consume 写入 gf_credit_ledger")
    void consume_writesCreditLedgerOnLocalPostgres() throws Exception {
        Assumptions.assumeTrue(
                "true".equalsIgnoreCase(System.getProperty("gaifan.pg.local")),
                "skip: set -Dgaifan.pg.local=true"
        );

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(JDBC_URL);
        dataSource.setUsername(JDBC_USER);
        dataSource.setPassword(JDBC_PASSWORD);
        dataSource.setDriverClassName("org.postgresql.Driver");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(3)).isTrue();
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Assumptions.assumeTrue(
                jdbcTemplate.queryForObject(
                        "select to_regclass('public.gf_credit_ledger') is not null",
                        Boolean.class
                ),
                "skip: gf_credit_ledger missing; run apply-gaifan-commercial-migrations.ps1"
        );

        CreditLedgerService creditLedgerService = new CreditLedgerService(jdbcTemplate);
        Integer before = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where tenant_id = 'demo-tenant'",
                Integer.class
        );

        String traceId = "e2e-pg-consume-" + System.currentTimeMillis();
        CreditConsumeResult result = creditLedgerService.consume(new CreditConsumeRequest(
                "demo-tenant",
                "demo-user",
                null,
                "video-insight",
                "video-insight.breakdown",
                "WEB",
                BigDecimal.ONE,
                "standard",
                traceId,
                "CreditLedgerConsumeLocalPgIT"
        ));

        assertThat(result.allowed()).isTrue();
        Integer traceHits = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where trace_id = ?",
                Integer.class,
                traceId
        );
        assertThat(traceHits).isGreaterThan(0);
        Integer after = jdbcTemplate.queryForObject(
                "select count(*) from gf_credit_ledger where tenant_id = 'demo-tenant'",
                Integer.class
        );
        assertThat(after).isGreaterThan(before);
    }
}
