package cn.gaifan.douyinOperations.module.ai.provider;

import org.springframework.stereotype.Component;

/**
 * DeepSeek 供应商适配 {@link AiProvider}。
 */
@Component
public class DeepSeekAiProvider implements AiProvider {

    private final DeepSeekProvider delegate;

    public DeepSeekAiProvider(DeepSeekProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public String providerCode() {
        return "deepseek";
    }

    @Override
    public String modelCode() {
        return delegate.getModel();
    }

    @Override
    public boolean isAvailable() {
        return delegate.isConfigured();
    }

    @Override
    public AiProviderResult chatWithUsage(String prompt) {
        return delegate.chatWithUsage(prompt);
    }
}
