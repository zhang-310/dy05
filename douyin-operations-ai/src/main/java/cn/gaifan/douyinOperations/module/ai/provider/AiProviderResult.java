package cn.gaifan.douyinOperations.module.ai.provider;

/**
 * AI Provider 调用结果（含 token 用量）。
 */
public record AiProviderResult(
        String output,
        String providerCode,
        String modelCode,
        long promptTokens,
        long completionTokens,
        boolean mock
) {
    public long totalTokens() {
        return promptTokens + completionTokens;
    }

    public static AiProviderResult mock(String output, String modelCode) {
        return new AiProviderResult(output, "mock", modelCode, 0, 0, true);
    }
}
