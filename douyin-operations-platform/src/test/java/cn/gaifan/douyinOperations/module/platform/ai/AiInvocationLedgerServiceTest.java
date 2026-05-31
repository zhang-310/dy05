package cn.gaifan.douyinOperations.module.platform.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiInvocationLedgerService 单测")
class AiInvocationLedgerServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AiInvocationLedgerService aiInvocationLedgerService;

    @Test
    @DisplayName("record 写入 gf_ai_invocation 与 gf_usage_ledger")
    void record_writesBothTables() {
        when(jdbcTemplate.queryForObject(
                eq("select count(*) from gf_tenant where tenant_id = ?"),
                eq(Integer.class),
                eq("demo-tenant")
        )).thenReturn(1);

        aiInvocationLedgerService.record(new AiInvocationRecord(
                "ai_req_test",
                "demo-tenant",
                "demo-user",
                "douyin-ops",
                "ai.chat",
                "mock",
                "mock-model",
                "ai.chat",
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.ONE,
                "trace-ai-test",
                "AI",
                true
        ));

        verify(jdbcTemplate, atLeastOnce()).update(
                argThat(sql -> sql.contains("gf_ai_invocation")),
                eq("ai_req_test"),
                eq("demo-tenant"),
                eq("demo-user"),
                eq("douyin-ops"),
                eq("ai.chat"),
                eq("mock"),
                eq("mock-model"),
                eq("ai.chat"),
                eq(new BigDecimal("10.0000")),
                eq(new BigDecimal("1.0000")),
                eq("trace-ai-test"),
                any(OffsetDateTime.class)
        );
        verify(jdbcTemplate, atLeastOnce()).update(
                argThat(sql -> sql.contains("gf_usage_ledger")),
                argThat(id -> id != null && id.toString().startsWith("usage_ai_")),
                eq("demo-tenant"),
                eq("demo-user"),
                eq("douyin-ops"),
                eq("ai.chat"),
                eq("AI"),
                eq("ai_call"),
                eq(BigDecimal.ONE),
                eq(new BigDecimal("1.0000")),
                eq(new BigDecimal("1.0000")),
                eq("trace-ai-test"),
                any(OffsetDateTime.class)
        );
    }
}
