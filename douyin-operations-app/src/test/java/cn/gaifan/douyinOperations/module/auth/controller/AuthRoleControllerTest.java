package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.service.AuthRoleService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthRoleController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthRoleController 集成测试")
class AuthRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthRoleService authRoleService;

    @Test
    @DisplayName("查询角色列表 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 30);

        PageResultVO<AuthRoleVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(5L);
        pageResult.setList(List.of());

        when(authRoleService.search(any())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/auth/role/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    @DisplayName("查询角色列表（非管理员）- 应返回 2002")
    void search_asUser_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/role/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("查询角色列表（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/role/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取所有角色 - 应返回 200")
    void listAll_shouldReturn200() throws Exception {
        AuthRoleVO role1 = new AuthRoleVO();
        role1.setId(1L);
        role1.setRoleName("管理员");
        role1.setRoleCode("admin");

        AuthRoleVO role2 = new AuthRoleVO();
        role2.setId(2L);
        role2.setRoleName("普通用户");
        role2.setRoleCode("user");

        when(authRoleService.listAll()).thenReturn(List.of(role1, role2));

        mockMvc.perform(post("/api/v1/auth/role/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].roleName").value("管理员"))
                .andExpect(jsonPath("$.data[1].roleName").value("普通用户"));
    }

    @Test
    @DisplayName("获取角色详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        AuthRoleVO role = new AuthRoleVO();
        role.setId(1L);
        role.setRoleName("管理员");
        role.setRoleCode("admin");

        when(authRoleService.getById(eq(1L))).thenReturn(role);

        mockMvc.perform(post("/api/v1/auth/role/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.roleName").value("管理员"));
    }

    @Test
    @DisplayName("保存角色 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("roleName", "新角色");
        body.put("roleCode", "new_role");

        when(authRoleService.save(any())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/role/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除角色 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(authRoleService).deleteById(eq(1L));

        mockMvc.perform(post("/api/v1/auth/role/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取角色资源权限 - 应返回 200")
    void getResources_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("roleId", 1L);

        when(authRoleService.getResourceIdsByRoleId(eq(1L))).thenReturn(List.of(1L, 2L, 3L));

        mockMvc.perform(post("/api/v1/auth/role/resources")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0]").value(1))
                .andExpect(jsonPath("$.data[1]").value(2))
                .andExpect(jsonPath("$.data[2]").value(3));
    }

    @Test
    @DisplayName("授予角色资源权限 - 应返回 204")
    void saveResources_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("roleId", 1L);
        body.put("resourceIds", List.of(1L, 2L, 3L));

        doNothing().when(authRoleService).saveRoleResources(eq(1L), anyList());

        mockMvc.perform(post("/api/v1/auth/role/resources/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }
}
