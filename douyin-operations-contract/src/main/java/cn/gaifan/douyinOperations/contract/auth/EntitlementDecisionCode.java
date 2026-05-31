package cn.gaifan.douyinOperations.contract.auth;

public enum EntitlementDecisionCode {
    ALLOW,
    TENANT_REQUIRED,
    USER_REQUIRED,
    PRODUCT_DISABLED,
    FEATURE_DISABLED,
    NO_ENTITLEMENT,
    ENTITLEMENT_EXPIRED,
    CHANNEL_DENIED,
    QUOTA_EXHAUSTED,
    POLICY_DENIED
}
