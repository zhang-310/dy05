package cn.gaifan.douyinOperations.module.douyinapi.service.impl;

import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.entity.OAuthToken;
import cn.gaifan.douyinOperations.module.douyinapi.repository.OAuthTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OAuthTokenServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthTokenServiceImpl 单元测试")
class OAuthTokenServiceImplTest {

    @Mock
    private OAuthTokenRepository tokenRepository;

    @Mock
    private DouyinApiClient douyinApiClient;

    @InjectMocks
    private OAuthTokenServiceImpl oauthTokenService;

    private OAuthToken sampleToken;

    @BeforeEach
    void setUp() {
        sampleToken = new OAuthToken();
        sampleToken.setId(1L);
        sampleToken.setUserId(100L);
        sampleToken.setProvider("douyin");
        sampleToken.setOpenId("open123");
        sampleToken.setAccessToken("access");
        sampleToken.setRefreshToken("refresh");
        sampleToken.setExpiresAt(new Timestamp(System.currentTimeMillis() + 86400_000));
        sampleToken.setScope("user_info");
        sampleToken.setDeleted(0);
    }

    @Nested
    @DisplayName("refreshToken 方法测试")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshToken_token不存在_返回false")
        void refreshToken_tokenNotFound_returnsFalse() {
            when(tokenRepository.findByUserIdAndProviderAndDeleted(100L, "douyin", 0))
                    .thenReturn(Optional.empty());

            boolean result = oauthTokenService.refreshToken(100L, "douyin");

            assertFalse(result);
            verify(douyinApiClient, never()).refreshAccessToken(any());
        }

        @Test
        @DisplayName("refreshToken_refreshToken为空_返回false")
        void refreshToken_refreshTokenBlank_returnsFalse() {
            sampleToken.setRefreshToken("");
            when(tokenRepository.findByUserIdAndProviderAndDeleted(100L, "douyin", 0))
                    .thenReturn(Optional.of(sampleToken));

            boolean result = oauthTokenService.refreshToken(100L, "douyin");

            assertFalse(result);
            verify(douyinApiClient, never()).refreshAccessToken(any());
        }

        @Test
        @DisplayName("refreshToken_不支持的provider_返回false且不调用douyinApiClient")
        void refreshToken_unsupportedProvider_returnsFalseAndNoApiCall() {
            sampleToken.setProvider("wechat");
            when(tokenRepository.findByUserIdAndProviderAndDeleted(100L, "wechat", 0))
                    .thenReturn(Optional.of(sampleToken));

            boolean result = oauthTokenService.refreshToken(100L, "wechat");

            assertFalse(result);
            verify(douyinApiClient, never()).refreshAccessToken(any());
        }
    }
}
