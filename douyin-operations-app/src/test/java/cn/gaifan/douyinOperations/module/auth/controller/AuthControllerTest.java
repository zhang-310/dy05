package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import cn.gaifan.douyinOperations.module.auth.service.*;
import cn.gaifan.douyinOperations.module.auth.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试
 * 测试认证相关的核心功能：登录、个人资料、菜单、资源列表
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthController 集成测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthLoginService authLoginService;

    @MockBean
    private CaptchaService captchaService;

    @MockBean
    private AuthUserService authUserService;

    @MockBean
    private AuthMenuService authMenuService;

    @MockBean
    private AuthResourceService authResourceService;

    @MockBean
    private ApplicationEventPublisher applicationEventPublisher;

    @MockBean
    private AuthOAuthService authOAuthService;

    @MockBean
    private AuthTokenStore authTokenStore;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        // Mock 登录返回
        LoginResultVO loginResult = new LoginResultVO();
        loginResult.setToken("test-token-123");
        loginResult.setUserId(1L);
        loginResult.setUsername("admin");
        loginResult.setNickname("管理员");
        loginResult.setRoleCode("ADMIN");

        when(authLoginService.login(any(LoginVO.class), anyString()))
                .thenReturn(loginResult);

        // Mock token 验证 - 返回用户ID和角色
        when(authTokenStore.getUserId(anyString())).thenReturn(1L);
        when(authTokenStore.getRoleCode(anyString())).thenReturn("ADMIN");
        when(authTokenStore.isValid(anyString())).thenReturn(true);

        // 执行登录获取 token
        token = "test-token-123";
    }

    @Test
    @DisplayName("获取验证码 - 200")
    void captcha_shouldReturn200() throws Exception {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaId("captcha-123");
        captchaVO.setImageBase64("data:image/png;base64,iVBORw0KGgo...");

        when(captchaService.generate()).thenReturn(captchaVO);

        mockMvc.perform(post("/api/v1/auth/captcha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.captchaId").value("captcha-123"))
                .andExpect(jsonPath("$.data.imageBase64").exists());
    }

    @Test
    @DisplayName("用户登录 - 200")
    void login_shouldReturn200() throws Exception {
        LoginVO loginVO = new LoginVO();
        loginVO.setUsername("admin");
        loginVO.setPassword("admin123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("test-token-123"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("获取个人资料 - 200")
    void profile_shouldReturn200() throws Exception {
        ProfileVO profileVO = new ProfileVO();
        profileVO.setId(1L);
        profileVO.setUsername("admin");
        profileVO.setNickname("管理员");
        profileVO.setEmail("admin@example.com");
        profileVO.setMobile("13800138000");

        when(authUserService.getProfile(1L)).thenReturn(profileVO);

        mockMvc.perform(post("/api/v1/auth/profile")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.nickname").value("管理员"));
    }

    @Test
    @DisplayName("更新个人资料 - 200")
    void profileUpdate_shouldReturn200() throws Exception {
        ProfileUpdateVO updateVO = new ProfileUpdateVO();
        updateVO.setNickname("新昵称");
        updateVO.setEmail("newemail@example.com");

        mockMvc.perform(post("/api/v1/auth/profile/update")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取菜单列表 - 200")
    void menu_shouldReturn200() throws Exception {
        List<MenuItemVO> menuList = new ArrayList<>();
        MenuItemVO menu1 = new MenuItemVO();
        menu1.setId(1L);
        menu1.setResourceName("首页");
        menu1.setResourceCode("dashboard");
        menuList.add(menu1);

        when(authMenuService.getMenuList(1L)).thenReturn(menuList);

        mockMvc.perform(post("/api/v1/auth/menu/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].resourceName").value("首页"));
    }

    @Test
    @DisplayName("获取资源代码列表 - 200")
    void resourceCodes_shouldReturn200() throws Exception {
        ResourceCodeVO resourceCodeVO = new ResourceCodeVO();
        resourceCodeVO.setMenus(List.of("dashboard", "product"));
        resourceCodeVO.setApis(List.of("user:read", "user:write"));
        resourceCodeVO.setButtons(List.of("product:create", "product:delete"));

        when(authResourceService.getResourceCodes(1L)).thenReturn(resourceCodeVO);

        mockMvc.perform(post("/api/v1/auth/resource/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.menus").isArray())
                .andExpect(jsonPath("$.data.apis[0]").value("user:read"));
    }

    @Test
    @DisplayName("未登录访问需要认证的接口 - 401")
    void withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("修改密码 - 200")
    void changePassword_shouldReturn200() throws Exception {
        ChangePasswordVO changePasswordVO = new ChangePasswordVO();
        changePasswordVO.setOldPassword("admin123");
        changePasswordVO.setNewPassword("newpass123");

        mockMvc.perform(post("/api/v1/auth/profile/change-password")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("发送短信验证码 - 200")
    void smsSend_shouldReturn200() throws Exception {
        SmsSendVO smsSendVO = new SmsSendVO();
        smsSendVO.setTarget("13800138000");
        smsSendVO.setType("login");

        mockMvc.perform(post("/api/v1/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(smsSendVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("忘记密码 - 200")
    void forgotPassword_shouldReturn200() throws Exception {
        ForgotPasswordVO forgotPasswordVO = new ForgotPasswordVO();
        forgotPasswordVO.setTarget("13800138000");
        forgotPasswordVO.setCode("123456");
        forgotPasswordVO.setNewPassword("newpass123");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotPasswordVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }
}
