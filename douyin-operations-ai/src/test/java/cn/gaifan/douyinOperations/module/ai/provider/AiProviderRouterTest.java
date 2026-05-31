package cn.gaifan.douyinOperations.module.ai.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiProviderRouterTest {

    @Test
    void resolvesMockByDefault() {
        AiProviderRouter router = new AiProviderRouter(new MockAiProvider(), new DeepSeekAiProvider(new DeepSeekProvider()), "mock");
        AiProvider provider = router.resolve("ai.chat");
        assertEquals("mock", provider.providerCode());
        assertNotNull(provider.chatWithUsage("hi"));
    }

    @Test
    void fallsBackToMockWhenDeepseekNotConfigured() {
        AiProviderRouter router = new AiProviderRouter(new MockAiProvider(), new DeepSeekAiProvider(new DeepSeekProvider()), "deepseek");
        AiProvider provider = router.resolve("ai.chat");
        assertEquals("mock", provider.providerCode());
    }
}
