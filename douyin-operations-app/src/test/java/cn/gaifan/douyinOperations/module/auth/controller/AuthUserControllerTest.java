package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.service.AuthUserService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginLogQueryVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginLogVO;
import cn.gaifan.douyinOperations.module.auth.vo.OnlineUserVO;
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
 * AuthUserController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthUserController 集成测试")
class AuthUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthUserService authUserService;

    @Test
    @DisplayName("查询用户列表 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 30);

        PageResultVO<AuthUserVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(authUserService.search(any(), anyLong(), anyBoolean())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/auth/user/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .requestAttr("organizationId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("查询用户列表（非管理员）- 应返回 2002")
    void search_asUser_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/user/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("查询用户列表（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/user/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取用户详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        AuthUserVO user = new AuthUserVO();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(authUserService.getById(eq(1L), anyLong(), anyBoolean())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/user/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .requestAttr("organizationId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("保存用户 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "newuser");
        body.put("email", "new@example.com");
        body.put("password", "password123");
        body.put("roleCode", "user");

        when(authUserService.save(any(), anyLong(), anyBoolean())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/user/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .requestAttr("organizationId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("禁用用户 - 应返回 200")
    void ban_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 2L);
        body.put("ban", true);
        body.put("reason", "违规操作");

        doNothing().when(authUserService).ban(eq(2L), eq(true), eq("违规操作"), anyLong(), anyBoolean());

        mockMvc.perform(post("/api/v1/auth/user/ban")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .requestAttr("organizationId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除用户 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 2L);

        doNothing().when(authUserService).deleteById(eq(2L), anyLong(), anyBoolean());

        mockMvc.perform(post("/api/v1/auth/user/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .requestAttr("organizationId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取登录日志（管理员）- 应返回 200")
    void loginLogs_admin_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 2L);
        body.put("page", 0);
        body.put("size", 20);

        LoginLogVO log1 = new LoginLogVO();
        log1.setUserId(2L);
        log1.setIp("192.168.1.1");

        PageResultVO<LoginLogVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(log1));
        pageResult.setPageNum(0);
        pageResult.setPageSize(20);

        when(authUserService.getLoginLogs(any(LoginLogQueryVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/auth/user/login-logs")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].ip").value("192.168.1.1"));
    }

    @Test
    @DisplayName("获取登录日志（普通用户）- 应返回 200")
    void loginLogs_user_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        LoginLogVO log1 = new LoginLogVO();
        log1.setUserId(1L);
        log1.setIp("192.168.1.2");

        PageResultVO<LoginLogVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(log1));
        pageResult.setPageNum(0);
        pageResult.setPageSize(20);

        when(authUserService.getLoginLogs(any(LoginLogQueryVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/auth/user/login-logs")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].userId").value(1));
    }

    @Test
    @DisplayName("获取在线用户 - 应返回 200")
    void online_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("withinMinutes", 30);
        body.put("maxSize", 100);

        OnlineUserVO user1 = new OnlineUserVO();
        user1.setUserId(1L);
        user1.setUsername("user1");

        when(authUserService.getOnlineUsers(eq(30), eq(100))).thenReturn(List.of(user1));

        mockMvc.perform(post("/api/v1/auth/user/online")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].username").value("user1"));
    }

    @Test
    @DisplayName("获取在线用户（非管理员）- 应返回 2002")
    void online_asUser_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/user/online")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }
}
