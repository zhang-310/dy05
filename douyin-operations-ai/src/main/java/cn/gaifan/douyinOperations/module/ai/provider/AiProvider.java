package cn.gaifan.douyinOperations.module.ai.provider;

/**
 * AI 供应商抽象。
 */
public interface AiProvider {

    String providerCode();

    String modelCode();

    boolean isAvailable();

    AiProviderResult chatWithUsage(String prompt);
}
