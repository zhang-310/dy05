package cn.gaifan.douyinOperations.contract.identity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityContextCompleteTest {

    @Test
    void anonymousAlwaysReturnsSameTenant() {
        var a1 = IdentityContext.anonymous();
        var a2 = IdentityContext.anonymous();
        assertEquals("default", a1.tenantId());
        assertEquals(a1.tenantId(), a2.tenantId());
    }

    @Test
    void authenticatedSetsAllFields() {
        var ctx = IdentityContext.authenticated("t1", 1L, "admin", 10L, "APP", "trace-1", "req-1");
        assertTrue(ctx.authenticated());
        assertEquals("USER_TOKEN", ctx.source());
        assertEquals("t1", ctx.tenantId());
        assertEquals(1L, ctx.userId());
        assertEquals("admin", ctx.roleCode());
        assertEquals(10L, ctx.organizationId());
        assertEquals("APP", ctx.channel());
        assertEquals("trace-1", ctx.traceId());
        assertEquals("req-1", ctx.requestId());
    }

    @Test
    void identityIsRecord() {
        var ctx = IdentityContext.authenticated("x", 2L, "r", null, "WEB", null, null);
        assertEquals(ctx, new IdentityContext("USER_TOKEN", true, "x", 2L, "r", null, "WEB", null, null, null));
    }

    @Test
    void commercialHeaderCarriesGfUserId() {
        var ctx = IdentityContext.commercialHeader("demo-tenant", "demo-user", "AI", "t-1", "r-1");
        assertTrue(ctx.authenticated());
        assertEquals("COMMERCIAL_HEADER", ctx.source());
        assertEquals("demo-user", ctx.gfUserId());
    }
}
