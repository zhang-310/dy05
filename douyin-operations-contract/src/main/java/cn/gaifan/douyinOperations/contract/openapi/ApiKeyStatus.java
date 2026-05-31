package cn.gaifan.douyinOperations.contract.openapi;

/**
 * API Key 状态。
 *
 * <p>这里只暴露状态，不暴露明文密钥。真实密钥只能在创建时展示一次，
 * 后续控制台和接口都应返回脱敏摘要。</p>
 */
public enum ApiKeyStatus {
    ACTIVE,
    DISABLED,
    EXPIRED,
    REVOKED
}
