package cn.gaifan.douyinOperations.module.platform.identity;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommercialIdentityBridgeGfUserTest {

    @Test
    void resolveUserIdPrefersGfUserId() {
        IdentityContext ctx = IdentityContext.commercialHeader("demo-tenant", "demo-user", "WEB", "t", "r");
        assertEquals("demo-user", CommercialIdentityBridge.resolveUserId(ctx));
    }
}
