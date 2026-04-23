package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveReport;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicImportService;
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EvolveController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("EvolveController 集成测试")
class EvolveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EvolveEngineService evolveEngineService;

    @MockBean
    private EvolveTopicImportService evolveTopicImportService;

    @MockBean
    private EvolveRoiService evolveRoiService;

    @MockBean
    private AiEvolveReportRepository reportRepository;

    @Test
    @DisplayName("手动触发进化 - 应返回 200")
    void trigger_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("kbId", 10L);
        body.put("evolveAngle", "viral_analysis");

        doNothing().when(evolveEngineService).runEvolution(eq(10L), eq("viral_analysis"));

        mockMvc.perform(post("/api/v1/ai/admin/evolve/trigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("进化任务已启动，可在任务历史中查看进度"));
    }

    @Test
    @DisplayName("手动触发进化（无参数）- 应返回 200")
    void trigger_withoutParams_shouldReturn200() throws Exception {
        doNothing().when(evolveEngineService).runEvolution(isNull(), isNull());

        mockMvc.perform(post("/api/v1/ai/admin/evolve/trigger")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("进化状态概览 - 应返回 200")
    void status_shouldReturn200() throws Exception {
        AiEvolveTask task1 = new AiEvolveTask();
        task1.setId(1L);
        task1.setTaskNo("TASK001");
        task1.setStatus("completed");

        AiEvolveTopic topic1 = new AiEvolveTopic();
        topic1.setId(1L);
        topic1.setTopic("测试主题");

        when(evolveEngineService.listRecentTasks(eq(10))).thenReturn(List.of(task1));
        when(evolveEngineService.listTopics(isNull(), isNull(), eq(false))).thenReturn(List.of(topic1));
        when(evolveEngineService.getScoreTrend(eq(7))).thenReturn(List.of());
        when(evolveEngineService.getTopicDistribution()).thenReturn(List.of());
        when(evolveRoiService.getRoiMetrics(eq(10))).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/ai/admin/evolve/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.topicCount").value(1));
    }

    @Test
    @DisplayName("主题池列表 - 应返回 200")
    void listTopics_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("kbId", 10L);

        AiEvolveTopic topic1 = new AiEvolveTopic();
        topic1.setId(1L);
        topic1.setTopic("测试主题");
        topic1.setKbId(10L);

        when(evolveEngineService.listTopics(eq(10L), isNull(), eq(false))).thenReturn(List.of(topic1));

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].topic").value("测试主题"));
    }

    @Test
    @DisplayName("主题池列表（全局）- 应返回 200")
    void listTopics_global_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("scopeGlobal", true);

        AiEvolveTopic topic1 = new AiEvolveTopic();
        topic1.setId(1L);
        topic1.setTopic("全局主题");

        when(evolveEngineService.listTopics(isNull(), isNull(), eq(true))).thenReturn(List.of(topic1));

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("新增主题 - 应返回 200")
    void saveTopic_create_shouldReturn200() throws Exception {
        AiEvolveTopic topic = new AiEvolveTopic();
        topic.setTopic("新主题");
        topic.setCategory("basic");
        topic.setPriority(100);

        AiEvolveTopic saved = new AiEvolveTopic();
        saved.setId(1L);
        saved.setTopic("新主题");

        when(evolveEngineService.saveTopic(any(AiEvolveTopic.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topic)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("编辑主题 - 应返回 200")
    void saveTopic_update_shouldReturn200() throws Exception {
        AiEvolveTopic topic = new AiEvolveTopic();
        topic.setId(1L);
        topic.setTopic("更新主题");
        topic.setPriority(200);

        when(evolveEngineService.saveTopic(any(AiEvolveTopic.class))).thenReturn(topic);

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topic)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("从文件导入主题 - 应返回 200")
    void importTopics_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("sourcePath", "/path/to/topics.json");
        body.put("kbId", 10L);

        EvolveTopicImportService.TopicImportResult result =
                new EvolveTopicImportService.TopicImportResult(10, 8, 2, List.of());

        when(evolveTopicImportService.importFromFile(eq("/path/to/topics.json"), eq(10L))).thenReturn(result);

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/import-from-file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("从文件导入主题（缺少 sourcePath）- 应返回 1001")
    void importTopics_missingSourcePath_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("kbId", 10L);

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/import-from-file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("sourcePath 不能为空"));
    }

    @Test
    @DisplayName("删除主题（POST）- 应返回 200")
    void deleteTopicPost_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(evolveEngineService).deleteTopic(eq(1L));

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @DisplayName("删除主题（POST，缺少 id）- 应返回 1001")
    void deleteTopicPost_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/evolve/topic/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("id 不能为空"));
    }

    @Test
    @DisplayName("删除主题（DELETE）- 应返回 200")
    void deleteTopic_shouldReturn200() throws Exception {
        doNothing().when(evolveEngineService).deleteTopic(eq(1L));

        mockMvc.perform(delete("/api/v1/ai/admin/evolve/topic/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @DisplayName("最近任务列表 - 应返回 200")
    void listTasks_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("limit", 20);

        AiEvolveTask task1 = new AiEvolveTask();
        task1.setId(1L);
        task1.setTaskNo("TASK001");

        when(evolveEngineService.listRecentTasks(eq(20))).thenReturn(List.of(task1));

        mockMvc.perform(post("/api/v1/ai/admin/evolve/task/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].taskNo").value("TASK001"));
    }

    @Test
    @DisplayName("最近任务列表（默认参数）- 应返回 200")
    void listTasks_defaults_shouldReturn200() throws Exception {
        when(evolveEngineService.listRecentTasks(eq(10))).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/ai/admin/evolve/task/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除进化任务 - 应返回 200")
    void deleteTask_shouldReturn200() throws Exception {
        doNothing().when(evolveEngineService).deleteTask(eq(1L));

        mockMvc.perform(delete("/api/v1/ai/admin/evolve/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @DisplayName("获取进化报告 - 应返回 200")
    void getReport_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 1L);

        AiEvolveReport report = new AiEvolveReport();
        report.setId(1L);
        report.setTaskId(1L);
        report.setReportTitle("测试报告");
        report.setFullContent("报告内容");

        when(reportRepository.findByTaskId(eq(1L))).thenReturn(Optional.of(report));

        mockMvc.perform(post("/api/v1/ai/admin/evolve/report/by-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.reportTitle").value("测试报告"));
    }

    @Test
    @DisplayName("获取进化报告（缺少 taskId）- 应返回 1001")
    void getReport_missingTaskId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/evolve/report/by-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("taskId 不能为空"));
    }

    @Test
    @DisplayName("获取进化报告（报告不存在）- 应返回 1005")
    void getReport_notFound_shouldReturn1005() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskId", 999L);

        when(reportRepository.findByTaskId(eq(999L))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/ai/admin/evolve/report/by-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1005));
    }
}
