package cn.gaifan.douyinOperations.module.dashboard.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.dashboard.service.RoleHomeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleHomeControllerTest {

    private RoleHomeService roleHomeService;
    private RoleHomeController controller;

    @BeforeEach
    void setUp() {
        roleHomeService = mock(RoleHomeService.class);
        controller = new RoleHomeController();
        ReflectionTestUtils.setField(controller, "roleHomeService", roleHomeService);
    }

    @Test
    void adminHome_returnsUnauthorizedWithoutUserId() {
        RESTResult<Map<String, Object>> result = controller.adminHome(new MockHttpServletRequest());

        assertThat(result.getStatus()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void adminHome_rejectsNonAdminRole() {
        MockHttpServletRequest request = request(7L, "talent", 99L);

        RESTResult<Map<String, Object>> result = controller.adminHome(request);

        assertThat(result.getStatus()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void orgHome_passesOwnerOrgAndRoleBoundaryToService() {
        MockHttpServletRequest request = request(10L, "institution", 88L);
        when(roleHomeService.buildHome("org", 10L, "institution", 88L))
                .thenReturn(Map.of("role", "org", "visibleOwnerIds", List.of(10L, 21L)));

        RESTResult<Map<String, Object>> result = controller.orgHome(request);

        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("role", "org");
        verify(roleHomeService).buildHome("org", 10L, "institution", 88L);
    }

    @Test
    void talentHome_passesTalentBoundaryToService() {
        MockHttpServletRequest request = request(33L, "talent", 66L);
        when(roleHomeService.buildHome("talent", 33L, "talent", 66L))
                .thenReturn(Map.of("role", "talent", "visibleOwnerIds", List.of(33L)));

        RESTResult<Map<String, Object>> result = controller.talentHome(request);

        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("role", "talent");
        verify(roleHomeService).buildHome("talent", 33L, "talent", 66L);
    }

    @Test
    void userHome_passesUserBoundaryToService() {
        MockHttpServletRequest request = request(44L, "user", null);
        when(roleHomeService.buildHome("user", 44L, "user", null))
                .thenReturn(Map.of("role", "user", "visibleOwnerIds", List.of(44L)));

        RESTResult<Map<String, Object>> result = controller.userHome(request);

        assertThat(result.getStatus()).isEqualTo(200);
        assertThat(result.getData()).containsEntry("role", "user");
        verify(roleHomeService).buildHome("user", 44L, "user", null);
    }

    @Test
    void userHome_rejectsTalentRoleToAvoidCrossRoleData() {
        HttpServletRequest request = request(33L, "talent", null);

        RESTResult<Map<String, Object>> result = controller.userHome(request);

        assertThat(result.getStatus()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void orgHome_rejectsAdminRoleToAvoidGlobalScopeLeak() {
        HttpServletRequest request = request(1L, "admin", null);

        RESTResult<Map<String, Object>> result = controller.orgHome(request);

        assertThat(result.getStatus()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    private static MockHttpServletRequest request(Long userId, String roleCode, Long orgId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("roleCode", roleCode);
        if (orgId != null) {
            request.setAttribute("organizationId", orgId);
        }
        return request;
    }
}
