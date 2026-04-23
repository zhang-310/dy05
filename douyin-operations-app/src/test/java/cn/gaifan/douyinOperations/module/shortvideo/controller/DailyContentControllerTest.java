package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDailyBatch;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyContentService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DailyContentController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DailyContentController 集成测试")
class DailyContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DailyContentService dailyContentService;

    @Test
    @DisplayName("生成每日批量内容 - 应返回 200")
    void generateBatch_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("personaId", 1L);
        body.put("sourceType", "hot_topic");
        body.put("batchSize", 3);

        SvDailyBatch batch = new SvDailyBatch();
        batch.setId(1L);
        batch.setOwnerId(1L);
        batch.setStatus("processing");
        batch.setBatchSize(3);

        when(dailyContentService.generateBatch(eq(1L), eq(1L), eq("hot_topic"), eq(3)))
                .thenReturn(batch);

        mockMvc.perform(post("/api/v1/short-video/daily/generate-batch")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("processing"));
    }

    @Test
    @DisplayName("查询批次状态 - 应返回 200")
    void batchStatus_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("batchId", 1L);

        SvDailyBatch batch = new SvDailyBatch();
        batch.setId(1L);
        batch.setStatus("completed");

        when(dailyContentService.getBatchStatus(eq(1L))).thenReturn(batch);

        mockMvc.perform(post("/api/v1/short-video/daily/batch-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("completed"));
    }

    @Test
    @DisplayName("查询批次状态（缺少 batchId）- 应返回 1001")
    void batchStatus_missingBatchId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/daily/batch-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("批次历史列表 - 应返回 200")
    void batchList_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        SvDailyBatch batch = new SvDailyBatch();
        batch.setId(1L);
        batch.setStatus("completed");

        PageResultVO<SvDailyBatch> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(batch));

        when(dailyContentService.listBatches(eq(1L), eq(0), eq(10))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/daily/batch-list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(1));
    }

    @Test
    @DisplayName("批次历史列表（无参数）- 应返回 200")
    void batchList_noParams_shouldReturn200() throws Exception {
        PageResultVO<SvDailyBatch> pageResult = new PageResultVO<>();
        pageResult.setTotal(0L);
        pageResult.setList(List.of());

        when(dailyContentService.listBatches(eq(1L), eq(0), eq(10))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/short-video/daily/batch-list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("生成每日批量内容（未登录）- 应返回 2001")
    void generateBatch_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sourceType", "hot_topic");

        mockMvc.perform(post("/api/v1/short-video/daily/generate-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
