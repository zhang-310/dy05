package cn.gaifan.douyinOperations.module.system.config;

import cn.gaifan.douyinOperations.module.system.service.ExternalApiConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExternalApiHealthCheckSchedulerTest {

    @Mock
    private ExternalApiConfigService externalApiConfigService;

    @InjectMocks
    private ExternalApiHealthCheckScheduler scheduler;

    @Test
    void checkAll_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.checkAll();

        verify(externalApiConfigService, never()).getAllEnabled();
    }

    @Test
    void healthUrl_shouldUseProviderSpecificHealthPath() throws Exception {
        Method healthUrl = ExternalApiHealthCheckScheduler.class
                .getDeclaredMethod("healthUrl", String.class, String.class);
        Method healthPath = ExternalApiHealthCheckScheduler.class
                .getDeclaredMethod("healthPath", String.class);
        Method method = ExternalApiHealthCheckScheduler.class
                .getDeclaredMethod("method", String.class);
        healthUrl.setAccessible(true);
        healthPath.setAccessible(true);
        method.setAccessible(true);

        String extraConfig = "{\"healthPath\":\"/api/tags\",\"method\":\"GET\"}";

        assertThat(healthPath.invoke(null, extraConfig)).isEqualTo("/api/tags");
        assertThat(method.invoke(null, extraConfig)).isEqualTo("GET");
        assertThat(healthUrl.invoke(null, "http://host.docker.internal:11434/", "/api/tags"))
                .isEqualTo("http://host.docker.internal:11434/api/tags");
    }

    @Test
    void method_shouldFallbackToHeadForUnsupportedMethods() throws Exception {
        Method method = ExternalApiHealthCheckScheduler.class
                .getDeclaredMethod("method", String.class);
        method.setAccessible(true);

        assertThat(method.invoke(null, "{\"method\":\"POST\"}")).isEqualTo("HEAD");
        assertThat(method.invoke(null, "{}")).isEqualTo("HEAD");
    }

    @Test
    void statusFromResponse_shouldValidateConfiguredJsonCode() throws Exception {
        Method statusFromResponse = ExternalApiHealthCheckScheduler.class
                .getDeclaredMethod("statusFromResponse", String.class, int.class, String.class);
        statusFromResponse.setAccessible(true);

        String extraConfig = "{\"successJsonCode\":200}";

        assertThat(statusFromResponse.invoke(null, extraConfig, 200, "{\"code\":200,\"msg\":\"success\"}"))
                .isEqualTo("healthy");
        assertThat(statusFromResponse.invoke(null, extraConfig, 200, "{\"code\":240,\"msg\":\"缺少API密钥参数\"}"))
                .isEqualTo("degraded");
        assertThat(statusFromResponse.invoke(null, extraConfig, 404, "{\"code\":404}"))
                .isEqualTo("degraded");
    }
}
