package cn.gaifan.douyinOperations.common.config;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.AuthPermissionService;
import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("AuthTokenFilter 鉴权过滤器测试")
class AuthTokenFilterTest {

    private AuthTokenFilter filter;
    private AuthTokenStore authTokenStore;
    private AuthPermissionService authPermissionService;

    @BeforeEach
    void setUp() {
        filter = new AuthTokenFilter();
        authTokenStore = mock(AuthTokenStore.class);
        authPermissionService = mock(AuthPermissionService.class);

        ReflectionTestUtils.setField(filter, "authTokenStore", authTokenStore);
        ReflectionTestUtils.setField(filter, "authPermissionService", authPermissionService);
        ReflectionTestUtils.setField(filter, "objectMapper", new ObjectMapper());
    }

    @Test
    void whitelistWebhookPathShouldPassThroughWithoutAuthCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/messaging/webhook/wecom");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(authTokenStore, authPermissionService);
    }

    @Test
    void protectedApiWithoutTokenShouldReturnUnauthorizedBusinessCode() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/auth/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("\"status\":" + ErrorCode.UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("未登录或登录已过期");
        verifyNoInteractions(authTokenStore, authPermissionService);
    }
}
