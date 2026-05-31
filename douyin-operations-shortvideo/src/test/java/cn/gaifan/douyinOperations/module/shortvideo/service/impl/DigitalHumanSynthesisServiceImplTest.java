package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class DigitalHumanSynthesisServiceImplTest {

    @InjectMocks
    private DigitalHumanSynthesisServiceImpl service;

    @Test
    void isConfigured_shouldBeFalseByDefaultWhenDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "mode", "disabled");
        ReflectionTestUtils.setField(service, "localVideoUrl", "https://example.com/local.mp4");

        assertThat(service.isConfigured()).isFalse();
        assertThat(service.synthesize(Map.of(), 1L, 1L)).isNull();
    }

    @Test
    void synthesize_shouldReturnLocalUrlOnlyWhenExplicitlyConfigured() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "mode", "local-url");
        ReflectionTestUtils.setField(service, "localVideoUrl", "https://example.com/local.mp4");

        assertThat(service.isConfigured()).isTrue();
        assertThat(service.synthesize(Map.of("scriptText", "hello"), 1L, 1L))
                .isEqualTo("https://example.com/local.mp4");
    }

    @Test
    void isConfigured_shouldRequireApiCredentialInApiMode() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "mode", "api");
        ReflectionTestUtils.setField(service, "heygenApiKey", "");
        ReflectionTestUtils.setField(service, "didApiKey", "");

        assertThat(service.isConfigured()).isFalse();

        ReflectionTestUtils.setField(service, "heygenApiKey", "heygen-key");

        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    void synthesize_shouldNotUseExampleImageWhenDidInputIsMissing() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "mode", "api");
        ReflectionTestUtils.setField(service, "heygenApiKey", "");
        ReflectionTestUtils.setField(service, "didApiKey", "did-key");

        assertThatCode(() -> service.synthesize(Map.of("scriptText", "hello"), 1L, 1L))
                .doesNotThrowAnyException();
        assertThat(service.synthesize(Map.of("scriptText", "hello"), 1L, 1L)).isNull();
    }
}
