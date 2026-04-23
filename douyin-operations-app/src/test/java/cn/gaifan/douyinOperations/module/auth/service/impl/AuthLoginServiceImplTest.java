package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.auth.entity.AuthLoginLog;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthLoginLogRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrganizationRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthVerifyCodeRepository;
import cn.gaifan.douyinOperations.module.auth.service.AuthLoginLogService;
import cn.gaifan.douyinOperations.module.auth.service.CaptchaService;
import cn.gaifan.douyinOperations.module.auth.vo.LoginVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthLoginServiceImpl 登录服务测试")
class AuthLoginServiceImplTest {

    @InjectMocks
    private AuthLoginServiceImpl loginService;

    @Mock
    private AuthUserRepository authUserRepository;
    @Mock
    private AuthLoginLogRepository authLoginLogRepository;
    @Mock
    private AuthLoginLogService authLoginLogService;
    @Mock
    private AuthVerifyCodeRepository authVerifyCodeRepository;
    @Mock
    private InMemoryAuthTokenStore authTokenStore;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private AuthOrganizationRepository authOrganizationRepository;

    private AuthUser buildUser(Long id, String username, String passwordHash) {
        AuthUser u = new AuthUser();
        u.setId(id);
        u.setUsername(username);
        u.setPasswordHash(passwordHash);
        u.setRoleCode("user");
        u.setStatus(0);
        u.setNickname("测试用户");
        u.setDeleted(0);
        u.setCreateTime(new Timestamp(System.currentTimeMillis()));
        return u;
    }

    @Nested
    @DisplayName("login 密码登录")
    class PasswordLoginTests {

        @Test
        void login_success_shouldReturnToken() {
            AuthUser user = buildUser(1L, "admin", "hashed_pw");
            when(authUserRepository.findByUsernameAndDeleted("admin", 0)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "hashed_pw")).thenReturn(true);
            when(authTokenStore.createToken(eq(1L), eq("user"), any())).thenReturn("tk_test123");
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(user);

            LoginVO vo = new LoginVO();
            vo.setUsername("admin");
            vo.setPassword("password123");

            LoginResultVO result = loginService.login(vo, "127.0.0.1");
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo("tk_test123");
            assertThat(result.getUserId()).isEqualTo(1L);
        }

        @Test
        void login_emptyUsername_shouldThrow() {
            LoginVO vo = new LoginVO();
            vo.setUsername("");
            vo.setPassword("password");

            assertThatThrownBy(() -> loginService.login(vo, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        void login_emptyPassword_shouldThrow() {
            LoginVO vo = new LoginVO();
            vo.setUsername("admin");
            vo.setPassword("");

            assertThatThrownBy(() -> loginService.login(vo, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        void login_userNotFound_shouldThrow() {
            when(authUserRepository.findByUsernameAndDeleted("ghost", 0)).thenReturn(Optional.empty());

            LoginVO vo = new LoginVO();
            vo.setUsername("ghost");
            vo.setPassword("password");

            assertThatThrownBy(() -> loginService.login(vo, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        void login_wrongPassword_shouldThrow() {
            AuthUser user = buildUser(1L, "admin", "hashed_pw");
            when(authUserRepository.findByUsernameAndDeleted("admin", 0)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);

            LoginVO vo = new LoginVO();
            vo.setUsername("admin");
            vo.setPassword("wrong");

            assertThatThrownBy(() -> loginService.login(vo, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        void login_bannedUser_shouldThrow() {
            AuthUser user = buildUser(1L, "banned", "hashed_pw");
            user.setStatus(1);
            when(authUserRepository.findByUsernameAndDeleted("banned", 0)).thenReturn(Optional.of(user));

            LoginVO vo = new LoginVO();
            vo.setUsername("banned");
            vo.setPassword("password");

            assertThatThrownBy(() -> loginService.login(vo, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("封禁");
        }
    }

    @Nested
    @DisplayName("login 验证码登录")
    class SmsLoginTests {

        @Test
        void login_sms_emptyTarget_shouldThrow() {
            LoginVO vo = new LoginVO();
            vo.setLoginType("sms");
            vo.setTarget("");
            vo.setCode("123456");

            assertThatThrownBy(() -> loginService.login(vo, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }

        @Test
        void login_sms_emptyCode_shouldThrow() {
            LoginVO vo = new LoginVO();
            vo.setLoginType("sms");
            vo.setTarget("13800138000");
            vo.setCode("");

            assertThatThrownBy(() -> loginService.login(vo, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不能为空");
        }
    }
}
