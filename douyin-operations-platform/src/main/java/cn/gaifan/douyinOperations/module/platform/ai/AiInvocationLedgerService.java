package cn.gaifan.douyinOperations.module.platform.ai;

import cn.gaifan.douyinOperations.common.id.Ids;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

/**
 * AI 网关调用台账：gf_ai_invocation 与 gf_usage_ledger 双写。
 */
@Service
public class AiInvocationLedgerService {

    private final JdbcTemplate jdbcTemplate;

    public AiInvocationLedgerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String newRequestId() {
        return Ids.compactUuid("ai_req");
    }

    public void record(AiInvocationRecord record) {
        if (record == null || record.tenantId() == null || record.tenantId().isBlank()) {
            return;
        }
        Integer tenantExists = jdbcTemplate.queryForObject(
                "select count(*) from gf_tenant where tenant_id = ?",
                Integer.class,
                record.tenantId()
        );
        if (tenantExists == null || tenantExists < 1) {
            return;
        }
        String requestId = record.requestId() != null ? record.requestId() : newRequestId();
        BigDecimal tokens = normalize(record.totalTokens());
        BigDecimal costCny = normalize(record.estimatedCostCny());
        BigDecimal retail = normalize(record.retailRevenueCny());
        String traceId = record.traceId() != null && !record.traceId().isBlank()
                ? record.traceId()
                : Ids.compactUuid("trace_ai");
        String userId = record.userId() != null ? record.userId() : "unknown";
        String channel = record.channel() != null && !record.channel().isBlank() ? record.channel() : "AI";

        jdbcTemplate.update(
                """
                        insert into gf_ai_invocation (request_id, tenant_id, user_id, product_code, feature_code,
                            provider_code, model_code, prompt_code, total_tokens, estimated_cost_cny, trace_id, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on conflict (request_id) do nothing
                        """,
                requestId,
                record.tenantId(),
                userId,
                record.productCode(),
                record.featureCode(),
                record.providerCode(),
                record.modelCode(),
                truncate(record.promptCode(), 128),
                tokens,
                costCny,
                traceId,
                OffsetDateTime.now()
        );

        if (record.success()) {
            jdbcTemplate.update(
                    """
                            insert into gf_usage_ledger (usage_id, tenant_id, user_id, product_code, feature_code,
                                channel, unit, amount, retail_revenue_cny, provider_cost_cny, trace_id, created_at)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            on conflict (usage_id) do nothing
                            """,
                    Ids.compactUuid("usage_ai"),
                    record.tenantId(),
                    userId,
                    record.productCode(),
                    record.featureCode(),
                    channel,
                    "ai_call",
                    BigDecimal.ONE,
                    retail,
                    costCny,
                    traceId,
                    OffsetDateTime.now()
            );
        }
    }

    private static BigDecimal normalize(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
