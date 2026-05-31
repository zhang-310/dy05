package cn.gaifan.douyinOperations.common.config;

import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthTokenFilterCommercialHeaderTest {

    @AfterEach
    void tearDown() {
        RequestIdentityHolder.clear();
    }

    @Test
    void resolveHeaderUserId_numeric() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "42");
        IdentityContext ctx = IdentityContext.commercialHeader("demo-tenant", "demo-user", "WEB", "t1", "r1");
        assertEquals(42L, AuthTokenFilter.resolveHeaderUserId(req, ctx));
    }

    @Test
    void resolveHeaderUserId_fromContextWhenHeaderMissing() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        IdentityContext ctx = IdentityContext.commercialHeader("demo-tenant", "7", "WEB", "t1", "r1");
        assertEquals(7L, AuthTokenFilter.resolveHeaderUserId(req, ctx));
    }

    @Test
    void resolveHeaderUserId_defaultsToOne() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        IdentityContext ctx = IdentityContext.commercialHeader("demo-tenant", "demo-user", "WEB", "t1", "r1");
        assertEquals(1L, AuthTokenFilter.resolveHeaderUserId(req, ctx));
    }
}
