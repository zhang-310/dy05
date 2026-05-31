package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.port.GovernancePort;
import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * GovernancePort 实现 — 治理审计委托
 */
@Service
public class GovernancePortImpl implements GovernancePort {

    @Resource private JdbcTemplate jdbcTemplate;

    @Override
    public void recordEvent(String action, String resource, String detail, boolean success) {
        try {
            jdbcTemplate.update(
                "INSERT INTO sys_audit_event (tenant_id, user_id, action, resource, detail, success, create_time) VALUES (?,?,?,?,?,?,?)",
                "default", 0, action, resource, detail, success, Timestamp.from(Instant.now()));
        } catch (Exception ignored) {}
    }

    @Override
    public void recordAiUsage(String featureCode, String model, long tokens, long costCredits) {
        try {
            jdbcTemplate.update(
                "INSERT INTO sys_usage_ledger (tenant_id, user_id, feature_code, model, tokens, cost_credits, create_time) VALUES (?,?,?,?,?,?,?)",
                "default", 0, featureCode, model, tokens, costCredits, Timestamp.from(Instant.now()));
        } catch (Exception ignored) {}
    }

    @Override
    public EntitlementDecision checkQuota(String tenantId, String featureCode) {
        try {
            var row = jdbcTemplate.queryForMap(
                "SELECT monthly_limit, monthly_used FROM sys_quota WHERE tenant_id=? AND feature_code=?",
                tenantId, featureCode);
            long limit = ((Number) row.get("monthly_limit")).longValue();
            long used = ((Number) row.get("monthly_used")).longValue();
            if (used >= limit) return EntitlementDecision.denied("system", featureCode, "配额耗尽");
            return EntitlementDecision.granted("system", featureCode);
        } catch (Exception e) {
            return EntitlementDecision.granted("system", featureCode);
        }
    }
}
