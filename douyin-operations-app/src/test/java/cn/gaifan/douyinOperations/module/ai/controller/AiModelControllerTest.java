package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.AiModelService;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelAdminVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelSaveVO;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * AiModelController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiModelController 集成测试")
class AiModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiModelService aiModelService;

    @Test
    @DisplayName("模型列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        AiModelAdminVO model1 = new AiModelAdminVO();
        model1.setId(1L);
        model1.setModelName("GPT-4");
        model1.setModelProvider("OpenAI");
        model1.setModelVersion("gpt-4-turbo");
        model1.setResolvedBaseUrl("https://api.openai.com");

        AiModelAdminVO model2 = new AiModelAdminVO();
        model2.setId(2L);
        model2.setModelName("Claude-3");
        model2.setModelProvider("Anthropic");
        model2.setModelVersion("claude-3-opus");
        model2.setResolvedBaseUrl("https://api.openai.com");

        when(aiModelService.listAllForAdmin()).thenReturn(List.of(model1, model2));

        mockMvc.perform(post("/api/v1/ai/admin/models/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].modelName").value("GPT-4"))
                .andExpect(jsonPath("$.data[1].modelName").value("Claude-3"));
    }

    @Test
    @DisplayName("连通性测试 - 应返回 200")
    void testConnection_shouldReturn200() throws Exception {
        Map<String, Object> ping = new LinkedHashMap<>();
        ping.put("success", true);
        ping.put("errorMsg", null);
        ping.put("tokensUsed", 1L);
        when(aiModelService.testConnection(eq(1L))).thenReturn(ping);

        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        mockMvc.perform(post("/api/v1/ai/admin/models/test-connection")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("保存模型（新增）- 应返回 200 且 data 非空（勿用 getSuccess(null)→204）")
    void save_create_shouldReturn204() throws Exception {
        AiModelSaveVO vo = new AiModelSaveVO();
        vo.setModelName("GPT-4");
        vo.setProvider("OpenAI");
        vo.setApiKey("sk-test");
        vo.setMaxTokens(4096);
        vo.setTemperature(new BigDecimal("0.7"));
        vo.setStatus(1);

        doNothing().when(aiModelService).save(any(AiModelSaveVO.class));

        mockMvc.perform(post("/api/v1/ai/admin/models/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ok").value(true));
    }

    @Test
    @DisplayName("保存模型（编辑）- 应返回 200 且含 id")
    void save_update_shouldReturn204() throws Exception {
        AiModelSaveVO vo = new AiModelSaveVO();
        vo.setId(1L);
        vo.setModelName("GPT-4-Updated");
        vo.setProvider("OpenAI");
        vo.setMaxTokens(8192);
        vo.setTemperature(new BigDecimal("0.8"));

        doNothing().when(aiModelService).save(any(AiModelSaveVO.class));

        mockMvc.perform(post("/api/v1/ai/admin/models/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("保存模型（缺少必填字段）- 应返回 1001")
    void save_missingRequired_shouldReturn1001() throws Exception {
        AiModelSaveVO vo = new AiModelSaveVO();
        vo.setModelName("GPT-4");
        // 缺少 provider, maxTokens, temperature

        mockMvc.perform(post("/api/v1/ai/admin/models/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除模型 - 应返回 200 且 data 非空")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(aiModelService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/ai/admin/models/delete")
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
    @DisplayName("删除模型（缺少 id）- 应返回 1001")
    void delete_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/models/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("id 不能为空"));
    }

    @Test
    @DisplayName("设为默认模型 - 应返回 200 且 data 非空")
    void setDefault_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(aiModelService).setDefault(eq(1L));

        mockMvc.perform(post("/api/v1/ai/admin/models/set-default")
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
    @DisplayName("设为默认模型（缺少 id）- 应返回 1001")
    void setDefault_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/ai/admin/models/set-default")
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
        mockMvc.perform(post("/api/v1/ai/admin/models/list")
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
        mockMvc.perform(post("/api/v1/ai/admin/models/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可访问"));
    }
}
