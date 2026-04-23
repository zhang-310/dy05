package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.storage.service.BosFileMetadataService;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShortVideoUploadController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ShortVideoUploadController 集成测试")
class ShortVideoUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BosStorageService bosStorageService;

    @MockBean
    private BosFileMetadataService bosFileMetadataService;

    @MockBean
    private SvProjectRepository projectRepository;

    @Test
    @DisplayName("列出参考图 - BOS 未配置 - 应返回错误")
    void listReferenceImages_bosNotConfigured_shouldReturnError() throws Exception {
        when(bosStorageService.isConfigured()).thenReturn(false);

        mockMvc.perform(post("/api/v1/short-video/upload/reference/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(3001));
    }

    @Test
    @DisplayName("列出参考图 - 应返回 200")
    void listReferenceImages_shouldReturn200() throws Exception {
        when(bosStorageService.isConfigured()).thenReturn(true);
        when(bosStorageService.listAllObjectKeys(anyString())).thenReturn(List.of(
                "1/references/characters/char1/image.jpg",
                "1/references/scenes/scene1/image.png"
        ));
        when(bosStorageService.getPublicUrl(anyString())).thenReturn("https://example.com/image.jpg");

        mockMvc.perform(post("/api/v1/short-video/upload/reference/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("列出参考图（未登录）- 应返回 2001")
    void listReferenceImages_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/upload/reference/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
