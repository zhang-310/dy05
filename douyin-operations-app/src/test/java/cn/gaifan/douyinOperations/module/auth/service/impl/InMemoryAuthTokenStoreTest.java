package cn.gaifan.douyinOperations.module.auth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryAuthTokenStore Token存储测试")
class InMemoryAuthTokenStoreTest {

    private InMemoryAuthTokenStore tokenStore;

    @BeforeEach
    void setUp() {
        tokenStore = new InMemoryAuthTokenStore();
    }

    @Nested
    @DisplayName("createToken 创建Token")
    class CreateTokenTests {

        @Test
        void createToken_shouldReturnNonNullToken() {
            String token = tokenStore.createToken(1L, "admin");
            assertThat(token).isNotNull().startsWith("tk_");
        }

        @Test
        void createToken_withOrganization_shouldStoreOrgId() {
            String token = tokenStore.createToken(1L, "admin", 100L);
            assertThat(tokenStore.getOrganizationId(token)).isEqualTo(100L);
        }

        @Test
        void createToken_differentCallsShouldReturnDifferentTokens() {
            String t1 = tokenStore.createToken(1L, "admin");
            String t2 = tokenStore.createToken(1L, "admin");
            assertThat(t1).isNotEqualTo(t2);
        }
    }

    @Nested
    @DisplayName("getUserId 获取用户ID")
    class GetUserIdTests {

        @Test
        void getUserId_validToken_shouldReturnUserId() {
            String token = tokenStore.createToken(42L, "user");
            assertThat(tokenStore.getUserId(token)).isEqualTo(42L);
        }

        @Test
        void getUserId_nullToken_shouldReturnNull() {
            assertThat(tokenStore.getUserId(null)).isNull();
        }

        @Test
        void getUserId_emptyToken_shouldReturnNull() {
            assertThat(tokenStore.getUserId("")).isNull();
        }

        @Test
        void getUserId_invalidToken_shouldReturnNull() {
            assertThat(tokenStore.getUserId("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("getRoleCode 获取角色编码")
    class GetRoleCodeTests {

        @Test
        void getRoleCode_validToken_shouldReturnRoleCode() {
            String token = tokenStore.createToken(1L, "super_admin");
            assertThat(tokenStore.getRoleCode(token)).isEqualTo("super_admin");
        }

        @Test
        void getRoleCode_nullToken_shouldReturnNull() {
            assertThat(tokenStore.getRoleCode(null)).isNull();
        }
    }

    @Nested
    @DisplayName("removeToken 移除Token")
    class RemoveTokenTests {

        @Test
        void removeToken_shouldInvalidateToken() {
            String token = tokenStore.createToken(1L, "admin");
            assertThat(tokenStore.isValid(token)).isTrue();
            tokenStore.removeToken(token);
            assertThat(tokenStore.isValid(token)).isFalse();
        }

        @Test
        void removeToken_null_shouldNotThrow() {
            tokenStore.removeToken(null);
        }

        @Test
        void removeToken_nonexistent_shouldNotThrow() {
            tokenStore.removeToken("nonexistent");
        }
    }

    @Nested
    @DisplayName("isValid Token有效性")
    class IsValidTests {

        @Test
        void isValid_validToken_shouldReturnTrue() {
            String token = tokenStore.createToken(1L, "admin");
            assertThat(tokenStore.isValid(token)).isTrue();
        }

        @Test
        void isValid_removedToken_shouldReturnFalse() {
            String token = tokenStore.createToken(1L, "admin");
            tokenStore.removeToken(token);
            assertThat(tokenStore.isValid(token)).isFalse();
        }

        @Test
        void isValid_nullToken_shouldReturnFalse() {
            assertThat(tokenStore.isValid(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("cleanExpiredTokens 清理过期Token")
    class CleanTests {

        @Test
        void cleanExpiredTokens_shouldNotRemoveValidTokens() {
            String token = tokenStore.createToken(1L, "admin");
            tokenStore.cleanExpiredTokens();
            assertThat(tokenStore.isValid(token)).isTrue();
        }
    }
}
