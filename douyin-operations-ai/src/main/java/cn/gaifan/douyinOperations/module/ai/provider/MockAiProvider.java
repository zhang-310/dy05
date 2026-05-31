package cn.gaifan.douyinOperations.module.ai.provider;

import org.springframework.stereotype.Component;

/**
 * 未配置真实 API Key 时的默认 Provider。
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String providerCode() {
        return "mock";
    }

    @Override
    public String modelCode() {
        return "mock-chat";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiProviderResult chatWithUsage(String prompt) {
        int len = prompt != null ? prompt.length() : 0;
        long est = Math.max(1, len / 4);
        String output = "[MOCK AI] 输入 " + len + " 字符。配置 deepseek API Key 或切换 provider 后返回真实结果。";
        return new AiProviderResult(output, providerCode(), modelCode(), est, est, true);
    }
}
