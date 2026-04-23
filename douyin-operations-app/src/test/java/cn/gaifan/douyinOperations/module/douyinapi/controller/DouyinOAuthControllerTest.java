package cn.gaifan.douyinOperations.module.douyinapi.controller;

import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.entity.OAuthToken;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DouyinOAuthController 集成测试")
class DouyinOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DouyinApiClient douyinApiClient;

    @MockBean
    private OAuthTokenService oauthTokenService;

    @MockBean
    private DouyinAccountRepository douyinAccountRepository;

    @Test
    @DisplayName("获取授权 URL (GET) - 应返回 200")
    void getAuthorizeUrl_shouldReturn200() throws Exception {
        when(douyinApiClient.getAuthUrl(anyString(), anyString()))
                .thenReturn("https://open.douyin.com/oauth/authorize?client_id=xxx&state=xxx");

        mockMvc.perform(get("/api/v1/douyin/oauth/authorize-url")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.authUrl").exists())
                .andExpect(jsonPath("$.data.state").exists());
    }

    @Test
    @DisplayName("获取授权 URL (GET)（未登录）- 应返回 2001")
    void getAuthorizeUrl_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(get("/api/v1/douyin/oauth/authorize-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取授权 URL (POST) - 应返回 200")
    void getAuthUrlPost_shouldReturn200() throws Exception {
        when(douyinApiClient.getAuthUrl(anyString(), anyString()))
                .thenReturn("https://open.douyin.com/oauth/authorize?client_id=xxx&state=xxx");

        mockMvc.perform(post("/api/v1/douyin/oauth/auth-url")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.authUrl").exists())
                .andExpect(jsonPath("$.data.state").exists());
    }

    @Test
    @DisplayName("获取授权 URL (POST)（未登录）- 应返回 2001")
    void getAuthUrlPost_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/oauth/auth-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("刷新 Token - 应返回 200")
    void refreshToken_shouldReturn200() throws Exception {
        DouyinApiClient.AccessTokenResponse tokenResponse =
                new DouyinApiClient.AccessTokenResponse("new-access-token", "new-refresh-token", 7200, "open-id-123");

        OAuthToken existingToken = new OAuthToken();
        existingToken.setScope("user_info,video.list");

        when(douyinApiClient.refreshAccessToken(eq("old-refresh-token")))
                .thenReturn(tokenResponse);
        when(oauthTokenService.getToken(eq(1L), eq("douyin")))
                .thenReturn(Optional.of(existingToken));
        doNothing().when(oauthTokenService).saveOrUpdateToken(anyLong(), anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());

        mockMvc.perform(post("/api/v1/douyin/oauth/refresh-token")
                        .requestAttr("userId", 1L)
                        .param("refreshToken", "old-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("刷新 Token（未登录）- 应返回 2001")
    void refreshToken_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/oauth/refresh-token")
                        .param("refreshToken", "old-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("撤销授权 - 应返回 204")
    void revokeToken_shouldReturn204() throws Exception {
        doNothing().when(oauthTokenService).deleteToken(eq(1L), eq("douyin"));

        mockMvc.perform(post("/api/v1/douyin/oauth/revoke")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("撤销授权（未登录）- 应返回 2001")
    void revokeToken_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/douyin/oauth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("撤销授权（按账号）- 应返回 204")
    void revokeToken_byAccount_shouldReturn204() throws Exception {
        DouyinAccount account = new DouyinAccount();
        account.setUserId(2L);

        Map<String, Object> body = new HashMap<>();
        body.put("accountId", 1L);

        when(douyinAccountRepository.findById(eq(1L)))
                .thenReturn(Optional.of(account));
        doNothing().when(oauthTokenService).deleteToken(eq(2L), eq("douyin"));

        mockMvc.perform(post("/api/v1/douyin/oauth/revoke")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("查询 Token 状态（有效）- 应返回 200")
    void tokenStatus_valid_shouldReturn200() throws Exception {
        OAuthToken token = new OAuthToken();
        token.setExpiresAt(Timestamp.valueOf(LocalDateTime.now().plusDays(30)));

        Map<String, Object> body = new HashMap<>();

        when(oauthTokenService.getToken(eq(1L), eq("douyin")))
                .thenReturn(Optional.of(token));

        mockMvc.perform(post("/api/v1/douyin/oauth/token-status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("valid"));
    }

    @Test
    @DisplayName("查询 Token 状态（已过期）- 应返回 200")
    void tokenStatus_expired_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();

        when(oauthTokenService.getToken(eq(1L), eq("douyin")))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/douyin/oauth/token-status")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.status").value("expired"));
    }

    @Test
    @DisplayName("查询 Token 状态（未登录）- 应返回 2001")
    void tokenStatus_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/douyin/oauth/token-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("刷新 Token（按账号）- 应返回 204")
    void tokenRefresh_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();

        when(oauthTokenService.refreshToken(eq(1L), eq("douyin")))
                .thenReturn(true);

        mockMvc.perform(post("/api/v1/douyin/oauth/token-refresh")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("刷新 Token（按账号）（未登录）- 应返回 2001")
    void tokenRefresh_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/douyin/oauth/token-refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
