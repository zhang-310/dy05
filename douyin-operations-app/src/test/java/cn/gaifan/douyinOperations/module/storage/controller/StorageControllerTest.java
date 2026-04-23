package cn.gaifan.douyinOperations.module.storage.controller;

import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import cn.gaifan.douyinOperations.module.storage.vo.StorageFileVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("StorageController 集成测试")
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BosStorageService bosStorageService;

    @Test
    @DisplayName("检查 BOS 配置 - 应返回 200")
    void configured_shouldReturn200() throws Exception {
        when(bosStorageService.isConfigured()).thenReturn(true);

        mockMvc.perform(post("/api/v1/storage/configured")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("检查 BOS 配置（未登录）- 应返回 2001")
    void configured_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/storage/configured")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("检查 BOS 配置（非管理员）- 应返回 2002")
    void configured_forbidden_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/storage/configured")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("列出存储文件 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        StorageFileVO fileVO = new StorageFileVO();
        fileVO.setKey("1/test.mp4");
        fileVO.setSize(1024000L);
        fileVO.setUrl("https://example.com/test.mp4");

        when(bosStorageService.listUserFiles(eq(1L), anyString()))
                .thenReturn(List.of(fileVO));

        Map<String, String> body = new HashMap<>();
        body.put("prefixSuffix", "2026-03-01/");

        mockMvc.perform(post("/api/v1/storage/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].key").value("1/test.mp4"));
    }

    @Test
    @DisplayName("列出存储文件（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/storage/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("列出存储文件（非管理员）- 应返回 2002")
    void list_forbidden_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/storage/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("上传文件 - 应返回 200")
    void upload_shouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp4",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "file content".getBytes()
        );

        when(bosStorageService.isConfigured()).thenReturn(true);
        when(bosStorageService.upload(anyString(), any()))
                .thenReturn("https://example.com/1/test.mp4");

        mockMvc.perform(multipart("/api/v1/storage/upload")
                        .file(file)
                        .param("prefix", "uploads/")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.url").value("https://example.com/1/test.mp4"));
    }

    @Test
    @DisplayName("上传文件（未登录）- 应返回 2001")
    void upload_unauthorized_shouldReturn2001() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp4",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "file content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/storage/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("上传文件（非管理员）- 应返回 2002")
    void upload_forbidden_shouldReturn2002() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.mp4",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "file content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/storage/upload")
                        .file(file)
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("删除存储文件 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(bosStorageService).deleteObject(anyString(), eq(1L));

        Map<String, String> body = new HashMap<>();
        body.put("key", "1/test.mp4");

        mockMvc.perform(post("/api/v1/storage/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除存储文件（未登录）- 应返回 2001")
    void delete_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("key", "1/test.mp4");

        mockMvc.perform(post("/api/v1/storage/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("删除存储文件（非管理员）- 应返回 2002")
    void delete_forbidden_shouldReturn2002() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("key", "1/test.mp4");

        mockMvc.perform(post("/api/v1/storage/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("获取文件 URL - 应返回 200")
    void getUrl_shouldReturn200() throws Exception {
        when(bosStorageService.getPublicUrlForUser(eq("1/test.mp4"), eq(1L)))
                .thenReturn("https://example.com/1/test.mp4");

        Map<String, String> body = new HashMap<>();
        body.put("key", "1/test.mp4");

        mockMvc.perform(post("/api/v1/storage/url")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.url").value("https://example.com/1/test.mp4"));
    }

    @Test
    @DisplayName("获取文件 URL（未登录）- 应返回 2001")
    void getUrl_unauthorized_shouldReturn2001() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("key", "1/test.mp4");

        mockMvc.perform(post("/api/v1/storage/url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取文件 URL（非管理员）- 应返回 2002")
    void getUrl_forbidden_shouldReturn2002() throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("key", "1/test.mp4");

        mockMvc.perform(post("/api/v1/storage/url")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }
}
