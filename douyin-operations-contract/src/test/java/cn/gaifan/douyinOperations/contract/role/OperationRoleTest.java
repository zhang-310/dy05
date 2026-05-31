package cn.gaifan.douyinOperations.contract.role;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * OperationRole 单元测试
 */
class OperationRoleTest {

    @Test
    void allSixRoles() {
        assertEquals(6, OperationRole.values().length);
    }

    @Test
    void fromCode() {
        assertEquals(OperationRole.ANCHOR, OperationRole.fromCode("anchor"));
        assertEquals(OperationRole.DIRECTOR, OperationRole.fromCode("director"));
        assertEquals(OperationRole.SELECTOR, OperationRole.fromCode("selector"));
    }

    @Test
    void unknownCodeFallsBackToAdmin() {
        assertEquals(OperationRole.ADMIN, OperationRole.fromCode("unknown"));
    }

    @Test
    void displayNames() {
        assertEquals("主播", OperationRole.ANCHOR.displayName());
        assertEquals("中控人员", OperationRole.DIRECTOR.displayName());
        assertEquals("选品人员", OperationRole.SELECTOR.displayName());
    }
}
