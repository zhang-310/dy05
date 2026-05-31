package cn.gaifan.douyinOperations.contract.identity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * IdentityContext 单元测试
 */
class IdentityContextTest {

    @Test
    void anonymousContext() {
        IdentityContext ctx = IdentityContext.anonymous();
        assertFalse(ctx.authenticated());
        assertEquals("ANONYMOUS", ctx.source());
        assertEquals("default", ctx.tenantId());
        assertNull(ctx.userId());
    }

    @Test
    void authenticatedContext() {
        IdentityContext ctx = IdentityContext.authenticated(
                "org-1", 100L, "admin", 1L, "WEB", "trace-123", "req-456");
        assertTrue(ctx.authenticated());
        assertEquals("USER_TOKEN", ctx.source());
        assertEquals("org-1", ctx.tenantId());
        assertEquals(100L, ctx.userId());
        assertEquals("admin", ctx.roleCode());
        assertEquals("WEB", ctx.channel());
        assertEquals("trace-123", ctx.traceId());
    }
}
