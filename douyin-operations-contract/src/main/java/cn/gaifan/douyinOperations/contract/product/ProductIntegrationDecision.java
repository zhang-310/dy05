package cn.gaifan.douyinOperations.contract.product;

import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;

/**
 * 产品间能力调用决策。
 *
 * <p>调用被允许的前提是：存在启用的互调规则、发起产品授权通过、目标产品授权通过。
 * 这样抖音运营可以编排短视频洞察和数字人，但账务、授权、用量和审计仍归属到真实被调用产品。</p>
 */
public record ProductIntegrationDecision(
        boolean allowed,
        String decisionCode,
        String reason,
        ProductIntegrationRuleSummary rule,
        CommercialEntitlementDecision sourceDecision,
        CommercialEntitlementDecision targetDecision,
        String billingProductCode,
        String billingFeatureCode,
        boolean auditRequired
) {
}
