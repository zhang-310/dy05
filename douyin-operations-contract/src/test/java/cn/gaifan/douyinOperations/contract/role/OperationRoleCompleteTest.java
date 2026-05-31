package cn.gaifan.douyinOperations.contract.role;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperationRoleCompleteTest {

    @Test
    void allRolesHaveCode() {
        for (var role : OperationRole.values()) {
            assertNotNull(role.code(), "Missing code for " + role.name());
            assertFalse(role.code().isEmpty());
        }
    }

    @Test
    void adminIsDefault() {
        assertEquals(OperationRole.ADMIN, OperationRole.fromCode(""));
        assertEquals(OperationRole.ADMIN, OperationRole.fromCode(null));
    }

    @Test
    void eachRoleHasUniqueCode() {
        var codes = java.util.Arrays.stream(OperationRole.values())
                .map(OperationRole::code).distinct().count();
        assertEquals(OperationRole.values().length, codes);
    }
}
