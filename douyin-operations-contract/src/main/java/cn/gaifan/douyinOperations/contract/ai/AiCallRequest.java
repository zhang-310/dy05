package cn.gaifan.douyinOperations.contract.ai;

/**
 * AI 网关统一调用请求。
 */
public record AiCallRequest(
        String featureCode,
        String prompt,
        String tenantId,
        String userId,
        String channel,
        String traceId
) {
    public AiCallRequest {
        if (featureCode == null || featureCode.isBlank()) {
            featureCode = "ai.chat";
        }
    }
}
