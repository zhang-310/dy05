package cn.gaifan.douyinOperations.module.platform.payment;

import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.commerce.PaymentCreditGrantPort;
import cn.gaifan.douyinOperations.contract.credit.CreditTransactionType;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentCreditGrantAdapter implements PaymentCreditGrantPort {

    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private DeliveryLedgerService deliveryLedgerService;

    public PaymentCreditGrantAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void grantOnPayment(String tenantId, String userId, BigDecimal credits, String traceId, String reason) {
        if (tenantId == null || tenantId.isBlank() || credits == null || credits.signum() <= 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        insert into gf_tenant (tenant_id, tenant_name, status)
                        values (?, ?, 'ACTIVE')
                        on conflict (tenant_id) do nothing
                        """,
                tenantId, tenantId
        );
        int updated = jdbcTemplate.update(
                """
                        update gf_credit_account
                        set total_granted = total_granted + ?,
                            available_credits = available_credits + ?,
                            updated_at = current_timestamp
                        where tenant_id = ?
                        """,
                credits, credits, tenantId
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    """
                            insert into gf_credit_account (tenant_id, total_granted, available_credits, frozen_credits, used_credits, expired_credits)
                            values (?, ?, ?, 0, 0, 0)
                            """,
                    tenantId, credits, credits
            );
        }
        BigDecimal balance = jdbcTemplate.queryForObject(
                "select available_credits from gf_credit_account where tenant_id = ?",
                BigDecimal.class,
                tenantId
        );
        jdbcTemplate.update(
                """
                        insert into gf_credit_ledger (entry_id, tenant_id, user_id, product_code, feature_code, channel,
                            transaction_type, credits, balance_after, trace_id, reason)
                        values (?, ?, ?, 'platform', 'payment.grant', 'PAYMENT', ?, ?, ?, ?, ?)
                        on conflict (entry_id) do nothing
                        """,
                Ids.compactUuid("credit_grant"),
                tenantId,
                userId,
                CreditTransactionType.GRANT.name(),
                credits,
                balance,
                traceId != null ? traceId : Ids.compactUuid("trace"),
                reason != null ? reason : "payment fulfillment"
        );
        if (deliveryLedgerService != null && reason != null && reason.contains("douyin-ops")) {
            String deliveryTrace = traceId != null ? traceId : "payment-grant";
            deliveryLedgerService.recordDouyinOpsDelivery(tenantId, deliveryTrace, "PAID");
        }
    }
}
