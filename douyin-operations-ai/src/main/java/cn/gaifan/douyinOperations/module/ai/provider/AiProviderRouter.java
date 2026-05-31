package cn.gaifan.douyinOperations.module.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 按 {@code app.ai.default-provider} 与 feature 路由 AI Provider。
 */
@Component
public class AiProviderRouter {

    private final Map<String, AiProvider> providersByCode;
    private final String defaultProviderCode;

    public AiProviderRouter(
            MockAiProvider mockAiProvider,
            DeepSeekAiProvider deepSeekAiProvider,
            @Value("${app.ai.default-provider:mock}") String defaultProviderCode
    ) {
        this.providersByCode = new LinkedHashMap<>();
        this.providersByCode.put(mockAiProvider.providerCode(), mockAiProvider);
        this.providersByCode.put(deepSeekAiProvider.providerCode(), deepSeekAiProvider);
        this.defaultProviderCode = normalize(defaultProviderCode);
    }

    public AiProvider resolve(String featureCode) {
        String code = defaultProviderCode;
        if ("deepseek".equals(code)) {
            AiProvider deepseek = providersByCode.get("deepseek");
            if (deepseek != null && deepseek.isAvailable()) {
                return deepseek;
            }
            return providersByCode.get("mock");
        }
        AiProvider selected = providersByCode.get(code);
        if (selected != null && selected.isAvailable()) {
            return selected;
        }
        return providersByCode.get("mock");
    }

    private static String normalize(String code) {
        return code == null ? "mock" : code.trim().toLowerCase(Locale.ROOT);
    }
}
