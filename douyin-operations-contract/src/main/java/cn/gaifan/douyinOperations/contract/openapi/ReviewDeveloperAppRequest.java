package cn.gaifan.douyinOperations.contract.openapi;

import java.util.List;

/**
 * 开发者应用上线审核请求。
 *
 * <p>该请求只接收准入状态、白名单和审核备注，不接收 API Key、Secret、证书或任何第三方真实密钥。
 * 它用于把开放 API 应用从“已创建”推进到“可对外调用”的上线前风控壳。</p>
 */
public record ReviewDeveloperAppRequest(
        String tenantId,
        String reviewerUserId,
        DeveloperAppStatus status,
        List<String> ipAllowlist,
        List<String> callbackUrls,
        String reviewNote
) {
}
