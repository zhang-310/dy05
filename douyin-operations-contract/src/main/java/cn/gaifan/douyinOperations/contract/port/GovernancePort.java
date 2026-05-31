package cn.gaifan.douyinOperations.contract.port;

import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;

/**
 * 治理审计 Port — 跨模块审计能力
 */
public interface GovernancePort {
    void recordEvent(String action, String resource, String detail, boolean success);
    void recordAiUsage(String featureCode, String model, long tokens, long costCredits);
    EntitlementDecision checkQuota(String tenantId, String featureCode);
}
