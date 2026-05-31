package cn.gaifan.douyinOperations.contract.auth;

public record CommercialEntitlementDecision(
        boolean allowed,
        EntitlementDecisionCode decisionCode,
        String productCode,
        String featureCode,
        String reason,
        QuotaSnapshot quota,
        EntitlementSummary effectiveEntitlement,
        boolean auditRequired
) {
}
