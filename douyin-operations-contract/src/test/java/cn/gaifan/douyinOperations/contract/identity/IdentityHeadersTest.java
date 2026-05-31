package cn.gaifan.douyinOperations.contract.identity;

import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityHeadersTest {

    @Test
    void allHeadersDefined() {
        assertNotNull(IdentityHeaders.X_TENANT_ID);
        assertNotNull(IdentityHeaders.X_USER_ID);
        assertNotNull(IdentityHeaders.X_TRACE_ID);
        assertNotNull(IdentityHeaders.X_SERVICE_AUTH);
    }

    @Test
    void headersStartWithXDy() {
        assertTrue(IdentityHeaders.X_TENANT_ID.startsWith("X-Dy-"));
        assertTrue(IdentityHeaders.X_CHANNEL.startsWith("X-Dy-"));
    }
}
