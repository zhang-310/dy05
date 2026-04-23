package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.ViralRemakeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ViralRemakeController 集成测试")
class ViralRemakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ViralRemakeService viralRemakeService;

    @Test
    @DisplayName("AI 推荐二创方向 - 应返回 200")
    void recommend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);

        doNothing().when(viralRemakeService).recommendRemake(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/viral-remake/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("AI 推荐二创方向（缺少 viralVideoId）- 应返回 1001")
    void recommend_missingViralVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/viral-remake/recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("批量 AI 推荐 - 应返回 200")
    void batchRecommend_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scoreThreshold", 80.0);
        body.put("limit", 10);

        when(viralRemakeService.batchRecommend(eq(1L), eq(80.0), eq(10)))
                .thenReturn(5);

        mockMvc.perform(post("/api/v1/short-video/viral-remake/batch-recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @DisplayName("批量 AI 推荐（默认参数）- 应返回 200")
    void batchRecommend_defaultParams_shouldReturn200() throws Exception {
        when(viralRemakeService.batchRecommend(eq(1L), eq(70.0), eq(20)))
                .thenReturn(10);

        mockMvc.perform(post("/api/v1/short-video/viral-remake/batch-recommend")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(10));
    }

    @Test
    @DisplayName("运营确认二创方向 - 应返回 200")
    void confirm_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);
        body.put("remakeType", "product_intro");
        body.put("personaId", 1L);

        doNothing().when(viralRemakeService).confirmRemake(eq(1L), eq("product_intro"), eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/viral-remake/confirm")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("运营确认二创方向（缺少 viralVideoId）- 应返回 1001")
    void confirm_missingViralVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("remakeType", "product_intro");

        mockMvc.perform(post("/api/v1/short-video/viral-remake/confirm")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("生成二创脚本 - 应返回 200")
    void generateScript_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);
        body.put("scriptMode", "sop");

        when(viralRemakeService.generateRemakeScript(eq(1L), eq(1L), eq("sop")))
                .thenReturn(100L);

        mockMvc.perform(post("/api/v1/short-video/viral-remake/generate-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(100));
    }

    @Test
    @DisplayName("生成二创脚本（默认 scriptMode）- 应返回 200")
    void generateScript_defaultMode_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);

        when(viralRemakeService.generateRemakeScript(eq(1L), eq(1L), eq("sop")))
                .thenReturn(100L);

        mockMvc.perform(post("/api/v1/short-video/viral-remake/generate-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.scriptId").value(100));
    }

    @Test
    @DisplayName("生成二创脚本（缺少 viralVideoId）- 应返回 1001")
    void generateScript_missingViralVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/viral-remake/generate-script")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("分配拍摄任务 - 应返回 200")
    void assignTask_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);
        body.put("photographerId", 2L);
        body.put("shootDate", "2026-04-15");

        when(viralRemakeService.assignToShootingTask(eq(1L), eq(2L), eq("2026-04-15"), eq(1L)))
                .thenReturn(50L);

        mockMvc.perform(post("/api/v1/short-video/viral-remake/assign-task")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(50));
    }

    @Test
    @DisplayName("分配拍摄任务（缺少 viralVideoId）- 应返回 1001")
    void assignTask_missingViralVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("photographerId", 2L);

        mockMvc.perform(post("/api/v1/short-video/viral-remake/assign-task")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("标记完成 - 应返回 200")
    void complete_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("viralVideoId", 1L);

        doNothing().when(viralRemakeService).markCompleted(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/viral-remake/complete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("标记完成（缺少 viralVideoId）- 应返回 1001")
    void complete_missingViralVideoId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/viral-remake/complete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
