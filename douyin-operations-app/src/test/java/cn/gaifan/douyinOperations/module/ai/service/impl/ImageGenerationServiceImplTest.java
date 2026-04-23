package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.repository.AiImageGenerationRepository;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 图像生成服务单元测试（重点：saveImage BOS 上传与降级）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageGenerationServiceImpl 图像生成服务测试")
class ImageGenerationServiceImplTest {

    @InjectMocks
    private ImageGenerationServiceImpl imageGenerationService;

    @Mock
    private AiImageGenerationRepository imageGenerationRepository;

    @Mock
    private ConfigService configService;

    @Mock
    private BosStorageService bosStorageService;

    @Nested
    @DisplayName("saveImage 图像保存")
    class SaveImageTests {

        @Test
        void saveImage_whenBosNotConfigured_returnsPlaceholderUrl() throws Exception {
            ReflectionTestUtils.setField(imageGenerationService, "bosStorageService", null);
            String base64 = Base64.getEncoder().encodeToString("fake-png-data".getBytes());
            String result = (String) ReflectionTestUtils.invokeMethod(imageGenerationService, "saveImage", base64, 1L);
            assertThat(result).isNotNull().startsWith("/uploads/images/").contains("img_1_");
        }

        @Test
        void saveImage_whenBosConfigured_uploadsAndReturnsUrl() throws Exception {
            when(bosStorageService.isConfigured()).thenReturn(true);
            when(bosStorageService.uploadBytes(anyString(), any(byte[].class), eq("image/png")))
                    .thenReturn("https://cdn.example.com/ai-images/1/123.png");
            String base64 = Base64.getEncoder().encodeToString("fake-png-data".getBytes());
            String result = (String) ReflectionTestUtils.invokeMethod(imageGenerationService, "saveImage", base64, 1L);
            assertThat(result).isEqualTo("https://cdn.example.com/ai-images/1/123.png");
        }

        @Test
        void saveImage_whenBosFails_returnsPlaceholderFallback() throws Exception {
            when(bosStorageService.isConfigured()).thenReturn(true);
            when(bosStorageService.uploadBytes(anyString(), any(byte[].class), eq("image/png")))
                    .thenThrow(new RuntimeException("BOS 网络错误"));
            String base64 = Base64.getEncoder().encodeToString("fake-png-data".getBytes());
            String result = (String) ReflectionTestUtils.invokeMethod(imageGenerationService, "saveImage", base64, 1L);
            assertThat(result).isNotNull().startsWith("/uploads/images/");
        }

        @Test
        void saveImage_whenInputNull_returnsNull() throws Exception {
            String result = (String) ReflectionTestUtils.invokeMethod(imageGenerationService, "saveImage", (String) null, 1L);
            assertThat(result).isNull();
        }

        @Test
        void saveImage_whenInputBlank_returnsNull() throws Exception {
            String result = (String) ReflectionTestUtils.invokeMethod(imageGenerationService, "saveImage", "   ", 1L);
            assertThat(result).isNull();
        }
    }
}
