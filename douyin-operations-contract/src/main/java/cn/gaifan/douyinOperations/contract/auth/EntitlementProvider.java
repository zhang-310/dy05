package cn.gaifan.douyinOperations.contract.auth;

import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;

/**
 * 授权检查服务 SPI
 *
 * 判断当前身份是否有权访问某产品功能。
 */
public interface EntitlementProvider {

    /**
     * 检查授权
     * @param tenantId 租户 ID
     * @param userId 用户 ID
     * @param productCode 产品编码
     * @param featureCode 功能编码
     */
    EntitlementDecision check(String tenantId, Long userId, String productCode, String featureCode);
}
