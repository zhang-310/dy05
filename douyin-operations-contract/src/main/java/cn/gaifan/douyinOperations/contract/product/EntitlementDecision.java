package cn.gaifan.douyinOperations.contract.product;

/**
 * 授权决策结果
 *
 * gaifan-ops EntitlementDecision 的简化版本。
 * 用于 service role boundary filter 和 BFF 层检查当前身份是否有权访问某产品功能。
 *
 * @param granted    是否授权
 * @param productCode 产品编码
 * @param featureCode 功能编码
 * @param reason      拒绝原因（granted=false 时）
 * @param quotaRemaining 剩余配额（-1 表示不限）
 */
public record EntitlementDecision(
        boolean granted,
        String productCode,
        String featureCode,
        String reason,
        long quotaRemaining
) {
    public static EntitlementDecision granted(String productCode, String featureCode) {
        return new EntitlementDecision(true, productCode, featureCode, null, -1);
    }

    public static EntitlementDecision denied(String productCode, String featureCode, String reason) {
        return new EntitlementDecision(false, productCode, featureCode, reason, 0);
    }
}
