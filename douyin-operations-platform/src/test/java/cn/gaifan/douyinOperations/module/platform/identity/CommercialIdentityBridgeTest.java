package cn.gaifan.douyinOperations.module.platform.identity;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommercialIdentityBridgeTest {

    @Test
    void resolvesOrgTenant() {
        IdentityContext ctx = IdentityContext.authenticated("default", 1L, "ORG_ADMIN", 42L, "WEB", "t1", "r1");
        assertEquals("org-42", CommercialIdentityBridge.resolveTenantId(ctx));
        assertEquals("user-1", CommercialIdentityBridge.resolveUserId(ctx));
    }
}
