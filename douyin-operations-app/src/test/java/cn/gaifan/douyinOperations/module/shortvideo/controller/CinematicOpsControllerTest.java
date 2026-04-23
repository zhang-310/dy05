package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.service.CinematicGenerationLogReportService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SceneCameraMappingAdminService;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CinematicOpsController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CinematicOpsController 集成测试")
class CinematicOpsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SceneCameraMappingAdminService sceneCameraMappingAdminService;

    @MockBean
    private CinematicGenerationLogReportService cinematicGenerationLogReportService;

    @Test
    @DisplayName("场景-运镜映射列表 - 应返回 200")
    void mappingList_shouldReturn200() throws Exception {
        when(sceneCameraMappingAdminService.listAll()).thenReturn(List.of(
                Map.of("id", 1L, "sceneType", "product_showcase", "cameraMovement", "dolly_in")
        ));

        mockMvc.perform(post("/api/v1/short-video/cinematic/scene-camera-mapping/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("场景-运镜映射列表（未登录）- 应返回 2001")
    void mappingList_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/cinematic/scene-camera-mapping/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存场景-运镜映射 - 应返回 200")
    void mappingSave_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sceneType", "product_showcase");
        body.put("cameraMovement", "dolly_in");

        when(sceneCameraMappingAdminService.save(any())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/short-video/cinematic/scene-camera-mapping/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存场景-运镜映射（非管理员）- 应返回 2002")
    void mappingSave_forbidden_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sceneType", "product_showcase");

        mockMvc.perform(post("/api/v1/short-video/cinematic/scene-camera-mapping/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("删除场景-运镜映射 - 应返回 204")
    void mappingDelete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(sceneCameraMappingAdminService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/short-video/cinematic/scene-camera-mapping/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除场景-运镜映射（缺少 id）- 应返回 1001")
    void mappingDelete_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/cinematic/scene-camera-mapping/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("生成日志汇总 - 应返回 200")
    void generationLogSummary_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("yearMonth", "2026-04");

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGenerations", 100);
        summary.put("successCount", 95);
        summary.put("failureCount", 5);

        when(cinematicGenerationLogReportService.summarize(eq(1L), anyLong(), anyLong()))
                .thenReturn(summary);

        mockMvc.perform(post("/api/v1/short-video/cinematic/generation-log/summary")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalGenerations").value(100))
                .andExpect(jsonPath("$.data.successCount").value(95));
    }

    @Test
    @DisplayName("生成日志汇总（全局查询，管理员）- 应返回 200")
    void generationLogSummary_global_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("global", true);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGenerations", 1000);
        summary.put("successCount", 950);
        summary.put("failureCount", 50);

        when(cinematicGenerationLogReportService.summarize(isNull(), anyLong(), anyLong()))
                .thenReturn(summary);

        mockMvc.perform(post("/api/v1/short-video/cinematic/generation-log/summary")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalGenerations").value(1000));
    }

    @Test
    @DisplayName("生成日志汇总（未登录）- 应返回 2001")
    void generationLogSummary_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/cinematic/generation-log/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
