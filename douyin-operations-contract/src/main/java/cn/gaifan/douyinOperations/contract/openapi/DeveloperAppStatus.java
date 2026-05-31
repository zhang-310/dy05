package cn.gaifan.douyinOperations.contract.openapi;

/**
 * 开发者应用状态。
 *
 * <p>开放 API 的商业化接入必须先有应用主体，再把 API Key、授权、限流、
 * 用量和审计挂到该应用下，避免外部系统直接拿租户或用户身份裸调 MCP 能力。</p>
 */
public enum DeveloperAppStatus {
    /**
     * 可正常调用开放能力。
     */
    ACTIVE,

    /**
     * 暂停调用，常见于欠费、风控或主动停用。
     */
    PAUSED,

    /**
     * 待审核或资料未补齐。
     */
    PENDING_REVIEW
}
