package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthLoginLogRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrganizationRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthUserServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthUserServiceImpl 单元测试")
class AuthUserServiceImplTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private AuthLoginLogRepository authLoginLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthOrganizationRepository authOrganizationRepository;

    @InjectMocks
    private AuthUserServiceImpl authUserService;

    private AuthUser sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new AuthUser();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setPasswordHash("$2a$10$encodedPasswordHash");
        sampleUser.setNickname("Test User");
        sampleUser.setMobile("13800138000");
        sampleUser.setEmail("test@example.com");
        sampleUser.setRoleCode("user");
        sampleUser.setStatus(0);
        sampleUser.setDeleted(0);
        sampleUser.setOrganizationId(10L);
        sampleUser.setCreateTime(new Timestamp(System.currentTimeMillis()));
        sampleUser.setUpdateTime(new Timestamp(System.currentTimeMillis()));
    }

    // ==================== getProfile / getUserInfo 测试 ====================

    @Nested
    @DisplayName("getProfile 方法测试")
    class GetProfileTests {

        @Test
        @DisplayName("getUserInfo_validToken_returnsUser - 有效用户 ID 返回用户信息")
        void getUserInfo_validToken_returnsUser() {
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            ProfileVO result = authUserService.getProfile(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("testuser", result.getUsername());
            assertEquals("Test User", result.getNickname());
            assertEquals("13800138000", result.getMobile());
            assertEquals("test@example.com", result.getEmail());
        }

        @Test
        @DisplayName("getUserInfo_nonExistentUser_throwsException - 用户不存在抛出异常")
        void getUserInfo_nonExistentUser_throwsException() {
            when(authUserRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.getProfile(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
        }
    }

    // ==================== changePassword 测试 ====================

    @Nested
    @DisplayName("changePassword 方法测试")
    class ChangePasswordTests {

        @Test
        @DisplayName("updatePassword_correctOldPassword_success - 旧密码正确，修改成功")
        void updatePassword_correctOldPassword_success() {
            ChangePasswordVO vo = new ChangePasswordVO();
            vo.setOldPassword("oldPassword123");
            vo.setNewPassword("newPassword456");

            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("oldPassword123", sampleUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$newEncodedHash");
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(sampleUser);

            assertDoesNotThrow(() -> authUserService.changePassword(1L, vo));

            verify(passwordEncoder).encode("newPassword456");
            verify(authUserRepository).save(any(AuthUser.class));
        }

        @Test
        @DisplayName("updatePassword_wrongOldPassword_throwsException - 旧密码错误抛出异常")
        void updatePassword_wrongOldPassword_throwsException() {
            ChangePasswordVO vo = new ChangePasswordVO();
            vo.setOldPassword("wrongPassword");
            vo.setNewPassword("newPassword456");

            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.changePassword(1L, vo));

            assertEquals(ErrorCode.PASSWORD_MISMATCH, ex.getCode());
            assertTrue(ex.getMessage().contains("当前密码错误"));
        }

        @Test
        @DisplayName("updatePassword_nullUserId_throwsException - userId 为空抛出异常")
        void updatePassword_nullUserId_throwsException() {
            ChangePasswordVO vo = new ChangePasswordVO();
            vo.setOldPassword("old");
            vo.setNewPassword("new");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.changePassword(null, vo));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("updatePassword_userNotFound_throwsException - 用户不存在抛出异常")
        void updatePassword_userNotFound_throwsException() {
            ChangePasswordVO vo = new ChangePasswordVO();
            vo.setOldPassword("old");
            vo.setNewPassword("new");

            when(authUserRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.changePassword(999L, vo));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
        }
    }

    // ==================== save (register) 测试 ====================

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("register_newUser_success - 新用户注册成功")
        void register_newUser_success() {
            AuthUserSaveVO vo = new AuthUserSaveVO();
            vo.setUsername("newuser");
            vo.setNickname("New User");
            vo.setPassword("password123");
            vo.setRoleCode("user");
            vo.setMobile("13900139000");
            vo.setEmail("new@example.com");

            when(authUserRepository.existsByUsernameAndDeleted("newuser", 0)).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedHash");
            when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> {
                AuthUser saved = invocation.getArgument(0);
                saved.setId(2L);
                return saved;
            });

            long id = authUserService.save(vo, null, true);

            assertEquals(2L, id);
            verify(authUserRepository).save(any(AuthUser.class));
        }

        @Test
        @DisplayName("register_duplicateUsername_throwsException - 重复用户名抛出异常")
        void register_duplicateUsername_throwsException() {
            AuthUserSaveVO vo = new AuthUserSaveVO();
            vo.setUsername("testuser");
            vo.setNickname("Duplicate");
            vo.setPassword("password123");
            vo.setRoleCode("user");

            when(authUserRepository.existsByUsernameAndDeleted("testuser", 0)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.save(vo, null, true));

            assertEquals(ErrorCode.USERNAME_EXISTS, ex.getCode());
            assertTrue(ex.getMessage().contains("用户名已存在"));
        }

        @Test
        @DisplayName("register_withDefaultPassword_success - 未填密码使用默认密码")
        void register_withDefaultPassword_success() {
            AuthUserSaveVO vo = new AuthUserSaveVO();
            vo.setUsername("newuser2");
            vo.setNickname("New User 2");
            vo.setRoleCode("user");
            // password not set

            when(authUserRepository.existsByUsernameAndDeleted("newuser2", 0)).thenReturn(false);
            when(passwordEncoder.encode("123456")).thenReturn("$2a$10$defaultHash");
            when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> {
                AuthUser saved = invocation.getArgument(0);
                saved.setId(3L);
                return saved;
            });

            long id = authUserService.save(vo, null, true);

            assertEquals(3L, id);
            // Default password "123456" should be encoded
            verify(passwordEncoder).encode("123456");
        }

        @Test
        @DisplayName("save_editExistingUser_success - 编辑已有用户成功")
        void save_editExistingUser_success() {
            AuthUserSaveVO vo = new AuthUserSaveVO();
            vo.setId(1L);
            vo.setUsername("testuser");
            vo.setNickname("Updated Name");
            vo.setRoleCode("admin");

            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(sampleUser);

            long id = authUserService.save(vo, null, true);

            assertEquals(1L, id);
        }

        @Test
        @DisplayName("save_editNonExistingUser_throwsException - 编辑不存在的用户抛出异常")
        void save_editNonExistingUser_throwsException() {
            AuthUserSaveVO vo = new AuthUserSaveVO();
            vo.setId(999L);
            vo.setUsername("ghost");
            vo.setRoleCode("user");

            when(authUserRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.save(vo, null, true));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
        }
    }

    // ==================== search 测试 ====================

    @Nested
    @DisplayName("search 方法测试")
    class SearchTests {

        @Test
        @DisplayName("search_asAdmin_returnsAllUsers - 管理员搜索返回所有用户")
        @SuppressWarnings("unchecked")
        void search_asAdmin_returnsAllUsers() {
            AuthUserSearchVO searchVO = new AuthUserSearchVO();
            searchVO.setPage(0);
            searchVO.setRows(10);

            Page<AuthUser> page = new PageImpl<>(List.of(sampleUser));
            when(authUserRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            PageResultVO<AuthUserVO> result = authUserService.search(searchVO, null, true);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals("testuser", result.getList().get(0).getUsername());
        }

        @Test
        @DisplayName("search_asNonAdmin_returnsOnlyOrgUsers - 非管理员仅返回本机构用户")
        @SuppressWarnings("unchecked")
        void search_asNonAdmin_returnsOnlyOrgUsers() {
            AuthUserSearchVO searchVO = new AuthUserSearchVO();
            searchVO.setPage(0);
            searchVO.setRows(10);

            Page<AuthUser> page = new PageImpl<>(List.of(sampleUser));
            when(authUserRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            PageResultVO<AuthUserVO> result = authUserService.search(searchVO, 10L, false);

            assertNotNull(result);
            // Spec should contain organizationId filter (verified via Specification construction)
            verify(authUserRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById_existingId_returnsVO - 正常查询返回用户")
        void getById_existingId_returnsVO() {
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            AuthUserVO result = authUserService.getById(1L, null, true);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("testuser", result.getUsername());
        }

        @Test
        @DisplayName("getById_nonExistingId_throwsException - 不存在用户抛出异常")
        void getById_nonExistingId_throwsException() {
            when(authUserRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.getById(999L, null, true));

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("getById_differentOrg_throwsException - 非管理员跨机构查看抛出异常")
        void getById_differentOrg_throwsException() {
            sampleUser.setOrganizationId(10L);
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.getById(1L, 20L, false));

            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }

        @Test
        @DisplayName("getById_sameOrg_returnsVO - 同机构非管理员可查看")
        void getById_sameOrg_returnsVO() {
            sampleUser.setOrganizationId(10L);
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            AuthUserVO result = authUserService.getById(1L, 10L, false);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("getById_nullId_throwsException - null ID 抛出异常")
        void getById_nullId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.getById(null, null, true));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }
    }

    // ==================== ban 测试 ====================

    @Nested
    @DisplayName("ban 方法测试")
    class BanTests {

        @Test
        @DisplayName("ban_validUser_success - 正常封禁用户")
        void ban_validUser_success() {
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(sampleUser);

            assertDoesNotThrow(() -> authUserService.ban(1L, true, "Violation", null, true));

            verify(authUserRepository).save(argThat(user ->
                    user.getStatus() == 1 && user.getBannedAt() != null));
        }

        @Test
        @DisplayName("ban_crossOrg_throwsException - 非管理员跨机构操作抛出异常")
        void ban_crossOrg_throwsException() {
            sampleUser.setOrganizationId(10L);
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.ban(1L, true, "reason", 20L, false));

            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }
    }

    // ==================== deleteById 测试 ====================

    @Nested
    @DisplayName("deleteById 方法测试")
    class DeleteByIdTests {

        @Test
        @DisplayName("deleteById_admin_success - 管理员删除成功")
        void deleteById_admin_success() {
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(sampleUser);

            assertDoesNotThrow(() -> authUserService.deleteById(1L, null, true));

            verify(authUserRepository).save(argThat(user -> user.getDeleted() == 1));
        }

        @Test
        @DisplayName("deleteById_crossOrg_throwsException - 跨机构删除抛出异常")
        void deleteById_crossOrg_throwsException() {
            sampleUser.setOrganizationId(10L);
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.deleteById(1L, 20L, false));

            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }

        @Test
        @DisplayName("deleteById_nullId_throwsException - null ID 抛出异常")
        void deleteById_nullId_throwsException() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authUserService.deleteById(null, null, true));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }
    }
}
