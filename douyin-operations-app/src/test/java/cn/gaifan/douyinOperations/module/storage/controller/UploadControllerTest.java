package cn.gaifan.douyinOperations.module.storage.controller;

import cn.gaifan.douyinOperations.module.storage.service.UploadService;
import cn.gaifan.douyinOperations.module.storage.vo.*;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("UploadController 集成测试")
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UploadService uploadService;

    @Test
    @DisplayName("初始化上传 - 应返回 200")
    void initUpload_shouldReturn200() throws Exception {
        UploadInitVO initVO = new UploadInitVO();
        initVO.setOriginalFilename("test.mp4");
        initVO.setFileSize(1024000L);
        initVO.setFileMd5("abc123");
        initVO.setStorageKey("uploads/test.mp4");

        UploadInitResultVO resultVO = new UploadInitResultVO();
        resultVO.setUploadId("upload123");
        resultVO.setChunkSize(102400);
        resultVO.setTotalChunks(10);

        when(uploadService.initUpload(eq(1L), any(UploadInitVO.class)))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/storage/upload/init")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.uploadId").value("upload123"));
    }

    @Test
    @DisplayName("初始化上传（未登录）- 应返回 2001")
    void initUpload_unauthorized_shouldReturn2001() throws Exception {
        UploadInitVO initVO = new UploadInitVO();
        initVO.setOriginalFilename("test.mp4");
        initVO.setFileSize(1024000L);
        initVO.setFileMd5("abc123");
        initVO.setStorageKey("uploads/test.mp4");

        mockMvc.perform(post("/api/v1/storage/upload/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("上传分块 - 应返回 204")
    void uploadChunk_shouldReturn204() throws Exception {
        MockMultipartFile chunkFile = new MockMultipartFile(
                "chunk",
                "chunk.dat",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "chunk data".getBytes()
        );

        doNothing().when(uploadService).uploadChunk(
                eq(1L), eq("upload123"), eq(0), eq("md5"), any()
        );

        mockMvc.perform(multipart("/api/v1/storage/upload/chunk")
                        .file(chunkFile)
                        .param("uploadId", "upload123")
                        .param("chunkIndex", "0")
                        .param("chunkMd5", "md5")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("上传分块（未登录）- 应返回 2001")
    void uploadChunk_unauthorized_shouldReturn2001() throws Exception {
        MockMultipartFile chunkFile = new MockMultipartFile(
                "chunk",
                "chunk.dat",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "chunk data".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/storage/upload/chunk")
                        .file(chunkFile)
                        .param("uploadId", "upload123")
                        .param("chunkIndex", "0")
                        .param("chunkMd5", "md5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("查询已上传分块列表 - 应返回 200")
    void getUploadedChunks_shouldReturn200() throws Exception {
        List<Integer> chunks = Arrays.asList(0, 1, 2, 3);

        when(uploadService.getUploadedChunks(eq(1L), eq("upload123")))
                .thenReturn(chunks);

        mockMvc.perform(get("/api/v1/storage/upload/chunks")
                        .param("uploadId", "upload123")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    @Test
    @DisplayName("查询已上传分块列表（未登录）- 应返回 2001")
    void getUploadedChunks_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(get("/api/v1/storage/upload/chunks")
                        .param("uploadId", "upload123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("查询上传进度 - 应返回 200")
    void getProgress_shouldReturn200() throws Exception {
        UploadProgressVO progressVO = new UploadProgressVO();
        progressVO.setUploadId("upload123");
        progressVO.setTotalChunks(10);
        progressVO.setUploadedChunks(5);
        progressVO.setProgressPercent(50);

        when(uploadService.getProgress(eq(1L), eq("upload123")))
                .thenReturn(progressVO);

        mockMvc.perform(get("/api/v1/storage/upload/progress")
                        .param("uploadId", "upload123")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.progressPercent").value(50));
    }

    @Test
    @DisplayName("查询上传进度（未登录）- 应返回 2001")
    void getProgress_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(get("/api/v1/storage/upload/progress")
                        .param("uploadId", "upload123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("完成上传 - 应返回 200")
    void completeUpload_shouldReturn200() throws Exception {
        UploadCompleteVO completeVO = new UploadCompleteVO();
        completeVO.setUploadId("upload123");

        UploadCompleteResultVO resultVO = new UploadCompleteResultVO();
        resultVO.setUploadId("upload123");
        resultVO.setFileUrl("https://example.com/file.mp4");

        when(uploadService.completeUpload(eq(1L), eq("upload123")))
                .thenReturn(resultVO);

        mockMvc.perform(post("/api/v1/storage/upload/complete")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.uploadId").value("upload123"));
    }

    @Test
    @DisplayName("完成上传（未登录）- 应返回 2001")
    void completeUpload_unauthorized_shouldReturn2001() throws Exception {
        UploadCompleteVO completeVO = new UploadCompleteVO();
        completeVO.setUploadId("upload123");

        mockMvc.perform(post("/api/v1/storage/upload/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("取消上传 - 应返回 204")
    void cancelUpload_shouldReturn204() throws Exception {
        doNothing().when(uploadService).cancelUpload(eq(1L), eq("upload123"));

        mockMvc.perform(post("/api/v1/storage/upload/cancel")
                        .param("uploadId", "upload123")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("取消上传（未登录）- 应返回 2001")
    void cancelUpload_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/storage/upload/cancel")
                        .param("uploadId", "upload123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
