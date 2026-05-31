package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * 治理审计服务
 *
 * 记录关键操作到审计日志和用量台账。
 * 参考 gaifan-ops GovernanceLedgerService。
 */
@Service
public class GovernanceService {

    private static final Logger log = LoggerFactory.getLogger(GovernanceService.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 记录审计事件
     */
    public void recordEvent(String action, String resource, String detail, boolean success) {
        IdentityContext ctx = RequestIdentityHolder.current();
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_audit_event (tenant_id, user_id, action, resource, detail, success, create_time) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    ctx.tenantId(),
                    ctx.userId() != null ? ctx.userId() : 0,
                    action, resource, detail, success,
                    Timestamp.from(Instant.now()));
        } catch (Exception e) {
            log.warn("审计事件写入失败: action={}, error={}", action, e.getMessage());
        }
    }

    /**
     * 记录 AI 调用用量
     */
    public void recordAiUsage(String featureCode, String model, long tokens, long costCredits) {
        IdentityContext ctx = RequestIdentityHolder.current();
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_usage_ledger (tenant_id, user_id, feature_code, model, tokens, cost_credits, create_time) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    ctx.tenantId(),
                    ctx.userId() != null ? ctx.userId() : 0,
                    featureCode, model, tokens, costCredits,
                    Timestamp.from(Instant.now()));
        } catch (Exception e) {
            log.warn("用量记录写入失败: feature={}, error={}", featureCode, e.getMessage());
        }
    }
}
