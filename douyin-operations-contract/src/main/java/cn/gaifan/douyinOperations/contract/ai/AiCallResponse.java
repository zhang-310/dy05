package cn.gaifan.douyinOperations.contract.ai;

/**
 * AI 网关统一调用响应。
 */
public record AiCallResponse(
        boolean success,
        String output,
        String deniedReason,
        long costCredits,
        String requestId,
        String traceId,
        String providerCode,
        String modelCode
) {
    public static AiCallResponse success(String output, long costCredits, String requestId, String traceId,
                                         String providerCode, String modelCode) {
        return new AiCallResponse(true, output, null, costCredits, requestId, traceId, providerCode, modelCode);
    }

    public static AiCallResponse denied(String reason, String requestId, String traceId) {
        return new AiCallResponse(false, null, reason, 0, requestId, traceId, null, null);
    }
}
