package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.module.auth.entity.AuthOrgMember;
import cn.gaifan.douyinOperations.module.auth.entity.AuthOrganization;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.service.OrganizationService;
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrganizationController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("OrganizationController 集成测试")
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private AuthUserRepository authUserRepository;

    @Test
    @DisplayName("获取我的机构 - 应返回 200")
    void getMyOrg_shouldReturn200() throws Exception {
        AuthOrganization org = new AuthOrganization();
        org.setId(1L);
        org.setOrgName("测试机构");
        org.setOrgCode("TEST_ORG");
        org.setContactName("张三");
        org.setContactPhone("13800138000");
        org.setStatus(1);
        org.setOwnerId(1L);
        org.setCreateTime(new Timestamp(System.currentTimeMillis()));

        when(organizationService.getOrgByOwnerId(eq(1L))).thenReturn(org);

        mockMvc.perform(post("/api/v1/organization/my")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.orgName").value("测试机构"));
    }

    @Test
    @DisplayName("获取我的机构（未登录）- 应返回 2001")
    void getMyOrg_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/organization/my")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("创建机构 - 应返回 200")
    void createOrg_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("orgName", "新机构");
        body.put("orgCode", "NEW_ORG");
        body.put("contactName", "李四");
        body.put("contactPhone", "13900139000");

        AuthOrganization org = new AuthOrganization();
        org.setId(1L);
        org.setOrgName("新机构");

        when(organizationService.createOrg(eq(1L), eq("新机构"), eq("NEW_ORG"), eq("李四"), eq("13900139000")))
                .thenReturn(org);

        mockMvc.perform(post("/api/v1/organization/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "institution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.orgName").value("新机构"));
    }

    @Test
    @DisplayName("创建机构（非机构角色）- 应返回 2002")
    void createOrg_notInstitution_shouldReturn2002() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("orgName", "新机构");

        mockMvc.perform(post("/api/v1/organization/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("更新机构信息 - 应返回 204")
    void updateOrg_shouldReturn204() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("orgName", "更新后的机构");
        body.put("contactName", "王五");
        body.put("contactPhone", "13700137000");

        AuthOrganization org = new AuthOrganization();
        org.setId(1L);

        when(organizationService.getOrgByOwnerId(eq(1L))).thenReturn(org);
        doNothing().when(organizationService).updateOrg(eq(1L), eq(1L), eq("更新后的机构"), eq("王五"), eq("13700137000"));

        mockMvc.perform(post("/api/v1/organization/update")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取机构成员列表 - 应返回 200")
    void getMembers_shouldReturn200() throws Exception {
        AuthOrganization org = new AuthOrganization();
        org.setId(1L);

        AuthOrgMember member = new AuthOrgMember();
        member.setId(1L);
        member.setOrgId(1L);
        member.setUserId(2L);
        member.setRoleInOrg("member");
        member.setStatus(1);

        AuthUser user = new AuthUser();
        user.setId(2L);
        user.setUsername("member1");
        user.setNickname("成员1");

        when(organizationService.getOrgByOwnerId(eq(1L))).thenReturn(org);
        when(organizationService.getMembers(eq(1L))).thenReturn(List.of(member));
        when(authUserRepository.findById(eq(2L))).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/organization/members")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].username").value("member1"));
    }

    @Test
    @DisplayName("邀请达人加入机构 - 应返回 200")
    void invite_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("userId", 2L);

        AuthOrganization org = new AuthOrganization();
        org.setId(1L);

        AuthOrgMember member = new AuthOrgMember();
        member.setId(1L);

        when(organizationService.getOrgByOwnerId(eq(1L))).thenReturn(org);
        when(organizationService.inviteTalent(eq(1L), eq(1L), eq(2L))).thenReturn(member);

        mockMvc.perform(post("/api/v1/organization/invite")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("移除机构成员 - 应返回 204")
    void remove_shouldReturn204() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("userId", 2L);

        AuthOrganization org = new AuthOrganization();
        org.setId(1L);

        when(organizationService.getOrgByOwnerId(eq(1L))).thenReturn(org);
        doNothing().when(organizationService).removeMember(eq(1L), eq(1L), eq(2L));

        mockMvc.perform(post("/api/v1/organization/remove")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取达人的待处理邀请 - 应返回 200")
    void getInvitations_shouldReturn200() throws Exception {
        AuthOrgMember pending = new AuthOrgMember();
        pending.setId(1L);
        pending.setOrgId(1L);
        pending.setInvitedAt(new Timestamp(System.currentTimeMillis()));

        AuthOrganization org = new AuthOrganization();
        org.setId(1L);
        org.setOrgName("测试机构");

        when(organizationService.getPendingInvitations(eq(1L))).thenReturn(List.of(pending));
        when(organizationService.getOrgById(eq(1L))).thenReturn(org);

        mockMvc.perform(post("/api/v1/organization/invitations")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].orgName").value("测试机构"));
    }

    @Test
    @DisplayName("达人接受邀请 - 应返回 200")
    void acceptInvitation_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("memberId", 1L);

        doNothing().when(organizationService).acceptInvitation(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/organization/invitation/accept")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("达人拒绝邀请 - 应返回 200")
    void rejectInvitation_shouldReturn200() throws Exception {
        Map<String, Long> body = new HashMap<>();
        body.put("memberId", 1L);

        doNothing().when(organizationService).rejectInvitation(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/organization/invitation/reject")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("搜索达人 - 应返回 200")
    void searchTalents_shouldReturn200() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("keyword", "test");

        AuthUser user = new AuthUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setNickname("测试用户");
        user.setMobile("13800138000");

        when(authUserRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(user));

        mockMvc.perform(post("/api/v1/organization/search-talents")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].username").value("testuser"));
    }

    @Test
    @DisplayName("搜索达人（缺少关键词）- 应返回 1001")
    void searchTalents_missingKeyword_shouldReturn1001() throws Exception {
        Map<String, String> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/organization/search-talents")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
