package cn.gaifan.douyinOperations.module.ai.gateway;

import cn.gaifan.douyinOperations.module.ai.provider.DeepSeekProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiGatewayServiceProviderTest {

    @Test
    void deepSeekNotConfiguredByDefault() {
        var provider = new DeepSeekProvider();
        assertFalse(provider.isConfigured());
    }

    @Test
    void chatReturnsNullWhenNotConfigured() {
        var provider = new DeepSeekProvider();
        assertNull(provider.chat("test"));
    }
}
