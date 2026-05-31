package cn.gaifan.douyinOperations.module.platform.auth;

import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.auth.EntitlementDecisionCode;
import cn.gaifan.douyinOperations.contract.auth.EntitlementGrantType;
import cn.gaifan.douyinOperations.contract.auth.EntitlementScope;
import cn.gaifan.douyinOperations.contract.auth.EntitlementSummary;
import cn.gaifan.douyinOperations.contract.auth.QuotaRule;
import cn.gaifan.douyinOperations.contract.auth.QuotaSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商业化授权（读 gf_entitlement）；与 {@link cn.gaifan.douyinOperations.module.platform.service.EntitlementServiceImpl} 的 BFF 简化授权并存。
 */
@Service
public class PlatformEntitlementService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.credit.enforce:false}")
    private boolean creditEnforce;

    public PlatformEntitlementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CommercialEntitlementDecision check(EntitlementCheckRequest request) {
        BigDecimal amount = request.requestedAmount() == null ? BigDecimal.ONE : request.requestedAmount();
        QuotaRule rule = new QuotaRule("times", new BigDecimal("10000"), "monthly");
        QuotaSnapshot quota = new QuotaSnapshot("times", new BigDecimal("9999"), rule.monthlyLimit(), amount, rule.period());

        if (!creditEnforce) {
            return allow(request, quota, "演示/灰度：未开启 app.credit.enforce");
        }
        if (request.tenantId() == null || request.tenantId().isBlank()) {
            return deny(request, EntitlementDecisionCode.TENANT_REQUIRED, "缺少 tenantId", quota);
        }
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*) from gf_entitlement
                        where tenant_id = ? and product_code = ? and status = 'ACTIVE'
                          and valid_from <= current_date and valid_to >= current_date
                          and (feature_code is null or feature_code = ? or ? is null)
                        """,
                Integer.class,
                request.tenantId(),
                request.productCode(),
                request.featureCode(),
                request.featureCode()
        );
        if (count != null && count > 0) {
            return allow(request, quota, "gf_entitlement 有效授权");
        }
        return deny(request, EntitlementDecisionCode.NO_ENTITLEMENT, "租户暂无有效产品授权", quota);
    }

    private static CommercialEntitlementDecision allow(EntitlementCheckRequest request, QuotaSnapshot quota, String reason) {
        EntitlementSummary summary = new EntitlementSummary(
                "ent-bridge",
                EntitlementScope.TENANT,
                EntitlementGrantType.PRODUCT,
                request.tenantId(),
                request.userId(),
                request.productCode(),
                request.featureCode(),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1),
                List.of(normalizeChannel(request.channel()))
        );
        return new CommercialEntitlementDecision(
                true,
                EntitlementDecisionCode.ALLOW,
                request.productCode(),
                request.featureCode(),
                reason,
                quota,
                summary,
                true
        );
    }

    private static CommercialEntitlementDecision deny(
            EntitlementCheckRequest request,
            EntitlementDecisionCode code,
            String reason,
            QuotaSnapshot quota
    ) {
        return new CommercialEntitlementDecision(
                false,
                code,
                request.productCode(),
                request.featureCode(),
                reason,
                quota,
                null,
                false
        );
    }

    private static String normalizeChannel(String channel) {
        return channel == null || channel.isBlank() ? "WEB" : channel.toUpperCase();
    }
}
