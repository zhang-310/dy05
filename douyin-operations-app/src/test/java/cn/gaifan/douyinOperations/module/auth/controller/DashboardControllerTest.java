package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.module.auth.repository.AuthLoginLogRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrgMemberRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrganizationRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.system.repository.SysApiCallLogRepository;
import cn.gaifan.douyinOperations.module.system.repository.SysSyncLogRepository;
import cn.gaifan.douyinOperations.module.system.service.SystemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DashboardController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DashboardController 集成测试")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthUserRepository authUserRepository;

    @MockBean
    private AuthOrganizationRepository authOrganizationRepository;

    @MockBean
    private AuthOrgMemberRepository authOrgMemberRepository;

    @MockBean
    private LiveSessionRepository liveSessionRepository;

    @MockBean
    private AuthLoginLogRepository authLoginLogRepository;

    @MockBean
    private SystemService systemService;

    @MockBean
    private SysApiCallLogRepository sysApiCallLogRepository;

    @MockBean
    private SysSyncLogRepository sysSyncLogRepository;

    @Test
    @DisplayName("管理员 Dashboard - 应返回 200")
    void adminDashboard_shouldReturn200() throws Exception {
        when(authUserRepository.count()).thenReturn(100L);
        when(authOrganizationRepository.count()).thenReturn(10L);
        when(authUserRepository.count(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(50L);
        when(sysApiCallLogRepository.count()).thenReturn(1000L);
        when(sysSyncLogRepository.count()).thenReturn(200L);
        when(authLoginLogRepository.count()).thenReturn(500L);

        Map<String, Object> health = new HashMap<>();
        health.put("_overall", "UP");
        when(systemService.checkHealth()).thenReturn(health);

        Map<String, Object> apiStats = new HashMap<>();
        apiStats.put("totalCalls", 1000L);
        apiStats.put("successRate", 99.5);
        when(systemService.getApiLogStats(isNull(), isNull(), isNull())).thenReturn(apiStats);

        mockMvc.perform(post("/api/v1/dashboard/admin")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userCount").value(100))
                .andExpect(jsonPath("$.data.orgCount").value(10))
                .andExpect(jsonPath("$.data.talentCount").value(50));
    }

    @Test
    @DisplayName("管理员 Dashboard（未登录）- 应返回 2001")
    void adminDashboard_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("管理员 Dashboard（非管理员）- 应返回 2002")
    void adminDashboard_notAdmin_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/admin")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("机构 Dashboard - 应返回 200")
    void orgDashboard_shouldReturn200() throws Exception {
        when(authOrgMemberRepository.findMemberUserIdsByOrgOwnerId(eq(1L))).thenReturn(List.of(2L, 3L, 4L));
        when(liveSessionRepository.countByUserIdInAndDeleted(anyList(), eq(0))).thenReturn(15L);

        mockMvc.perform(post("/api/v1/dashboard/org")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "institution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.talentCount").value(3))
                .andExpect(jsonPath("$.data.liveCount").value(15));
    }

    @Test
    @DisplayName("机构 Dashboard（未登录）- 应返回 2001")
    void orgDashboard_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/org"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("机构 Dashboard（非机构角色）- 应返回 2002")
    void orgDashboard_notInstitution_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/org")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("达人 Dashboard - 应返回 200")
    void talentDashboard_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/talent")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "talent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.videoCount").value(0))
                .andExpect(jsonPath("$.data.liveCount").value(0));
    }

    @Test
    @DisplayName("达人 Dashboard（未登录）- 应返回 2001")
    void talentDashboard_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/talent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("达人 Dashboard（非达人角色）- 应返回 2002")
    void talentDashboard_notTalent_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/dashboard/talent")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }
}
