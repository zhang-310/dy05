package cn.gaifan.douyinOperations.common.aspect;

import cn.gaifan.douyinOperations.common.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogAspect 测试")
class AuditLogAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogAspect auditLogAspect;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("存在请求用户上下文时应返回真实 userId 和 username")
    void shouldResolveUserFromRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 23L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Long userId = ReflectionTestUtils.invokeMethod(auditLogAspect, "getCurrentUserId");
        String username = ReflectionTestUtils.invokeMethod(auditLogAspect, "getCurrentUsername");

        assertThat(userId).isEqualTo(23L);
        assertThat(username).isEqualTo("23");
    }

    @Test
    @DisplayName("无请求上下文时应回落为系统用户")
    void shouldFallbackToSystemUserWhenNoContext() {
        Long userId = ReflectionTestUtils.invokeMethod(auditLogAspect, "getCurrentUserId");
        String username = ReflectionTestUtils.invokeMethod(auditLogAspect, "getCurrentUsername");

        assertThat(userId).isEqualTo(0L);
        assertThat(username).isEqualTo("SYSTEM");
    }
}
