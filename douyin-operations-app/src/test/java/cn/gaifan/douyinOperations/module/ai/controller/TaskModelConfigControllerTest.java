package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.service.TaskModelConfigService;
import cn.gaifan.douyinOperations.module.ai.vo.TaskModelConfigRowVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TaskModelConfigController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("TaskModelConfigController 集成测试")
class TaskModelConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskModelConfigService taskModelConfigService;

    @Test
    @DisplayName("任务-模型配置列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        TaskModelConfigRowVO r1 = new TaskModelConfigRowVO();
        r1.setId(1L);
        r1.setTaskCode("short_video_script");
        r1.setPrimaryModelId(10L);

        TaskModelConfigRowVO r2 = new TaskModelConfigRowVO();
        r2.setId(2L);
        r2.setTaskCode("evolution");
        r2.setPrimaryModelId(20L);

        when(taskModelConfigService.listAll()).thenReturn(List.of(r1, r2));

        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].taskCode").value("short_video_script"))
                .andExpect(jsonPath("$.data[1].taskCode").value("evolution"));
    }

    @Test
    @DisplayName("任务-模型配置详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        TaskModelConfigRowVO row = new TaskModelConfigRowVO();
        row.setId(1L);
        row.setTaskCode("short_video_script");
        row.setPrimaryModelId(10L);

        when(taskModelConfigService.getRowById(eq(1L))).thenReturn(row);

        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskCode").value("short_video_script"))
                .andExpect(jsonPath("$.data.primaryModelId").value(10));
    }

    @Test
    @DisplayName("任务-模型配置详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("id 不能为空"));
    }

    @Test
    @DisplayName("新增任务-模型配置 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        AiTaskModelConfig config = new AiTaskModelConfig();
        config.setTaskCode("new_task");
        config.setPrimaryModelId(30L);

        when(taskModelConfigService.save(any(AiTaskModelConfig.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除任务-模型配置 - 应返回 200 且 data 非空")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(taskModelConfigService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("删除任务-模型配置（缺少 id）- 应返回 1001")
    void delete_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("id 不能为空"));
    }

    @Test
    @DisplayName("非管理员访问 - 应返回 2002")
    void asUser_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2002")
    void withoutAuth_shouldReturn2002() throws Exception {
        mockMvc.perform(post("/api/v1/ai/admin/task-model-config/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }
}
