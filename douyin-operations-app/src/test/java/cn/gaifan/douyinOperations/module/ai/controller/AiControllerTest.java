package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.module.ai.service.impl.AiServiceImpl;
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
 * AiController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AiController 集成测试")
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiServiceImpl aiService;

    @Test
    @DisplayName("AI 模型列表 - 应返回 200")
    void modelList_shouldReturn200() throws Exception {
        Map<String, Object> model1 = new HashMap<>();
        model1.put("id", 1L);
        model1.put("modelName", "GPT-4");
        model1.put("modelProvider", "OpenAI");

        Map<String, Object> model2 = new HashMap<>();
        model2.put("id", 2L);
        model2.put("modelName", "Claude-3");
        model2.put("modelProvider", "Anthropic");

        when(aiService.listModels(eq(1)))
                .thenReturn(List.of(model1, model2));

        mockMvc.perform(post("/api/v1/ai/model/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].modelName").value("GPT-4"))
                .andExpect(jsonPath("$.data[1].modelName").value("Claude-3"));
    }

    @Test
    @DisplayName("AI 模型详情 - 应返回 200")
    void modelGet_shouldReturn200() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("id", 1L);
        model.put("modelName", "GPT-4");
        model.put("modelProvider", "OpenAI");
        model.put("maxTokens", 8192);

        when(aiService.getModelById(eq(1L)))
                .thenReturn(model);

        mockMvc.perform(post("/api/v1/ai/model/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.modelName").value("GPT-4"))
                .andExpect(jsonPath("$.data.maxTokens").value(8192));
    }

    @Test
    @DisplayName("新增 AI 模型 - 应返回 200")
    void modelSave_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("modelName", "GPT-4-Turbo");
        body.put("modelProvider", "OpenAI");
        body.put("modelVersion", "gpt-4-turbo-2024-04-09");
        body.put("maxTokens", 128000);
        body.put("temperature", 0.7);
        body.put("status", 1);

        when(aiService.saveModel(isNull(), eq("GPT-4-Turbo"), eq("OpenAI"),
                eq("gpt-4-turbo-2024-04-09"), isNull(), eq(128000), eq(0.7), eq(1)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/model/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("新增 AI 模型（缺少必填字段）- 应返回 1001")
    void modelSave_withoutRequiredFields_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("modelName", "GPT-4");

        mockMvc.perform(post("/api/v1/ai/model/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("modelName/modelProvider/modelVersion 不能为空"));
    }

    @Test
    @DisplayName("删除 AI 模型 - 应返回 204")
    void modelDelete_shouldReturn204() throws Exception {
        doNothing().when(aiService).deleteModel(eq(1L));

        mockMvc.perform(post("/api/v1/ai/model/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("生成任务列表 - 应返回 200")
    void taskList_shouldReturn200() throws Exception {
        Map<String, Object> task1 = new HashMap<>();
        task1.put("id", 1L);
        task1.put("taskType", "text-generation");
        task1.put("taskStatus", 2);

        cn.gaifan.douyinOperations.common.vo.PageResultVO<Map<String, Object>> pageResult =
                new cn.gaifan.douyinOperations.common.vo.PageResultVO<>(1L, List.of(task1), 0, 20);

        when(aiService.searchTasks(eq(1L), eq("text-generation"), eq(2), eq(0), eq(20)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/ai/task/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("page", "0")
                        .param("rows", "20")
                        .param("taskType", "text-generation")
                        .param("taskStatus", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].taskType").value("text-generation"));
    }

    @Test
    @DisplayName("生成任务详情 - 应返回 200")
    void taskGet_shouldReturn200() throws Exception {
        Map<String, Object> task = new HashMap<>();
        task.put("id", 1L);
        task.put("taskType", "text-generation");
        task.put("inputContent", "生成一篇文章");
        task.put("outputContent", "这是生成的文章内容");

        when(aiService.getTaskById(eq(1L)))
                .thenReturn(task);

        mockMvc.perform(post("/api/v1/ai/task/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.taskType").value("text-generation"));
    }

    @Test
    @DisplayName("创建生成任务 - 应返回 200")
    void taskCreate_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("taskType", "text-generation");
        body.put("inputContent", "生成一篇关于AI的文章");
        body.put("prompt", "请生成一篇专业的AI技术文章");
        body.put("modelUsed", "GPT-4");

        when(aiService.createTask(eq(1L), eq("text-generation"),
                eq("生成一篇关于AI的文章"), eq("请生成一篇专业的AI技术文章"), eq("GPT-4")))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/task/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("创建生成任务（缺少 taskType）- 应返回 1001")
    void taskCreate_withoutTaskType_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("inputContent", "生成内容");

        mockMvc.perform(post("/api/v1/ai/task/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("taskType 不能为空"));
    }

    @Test
    @DisplayName("完成生成任务 - 应返回 204")
    void taskComplete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("outputContent", "生成的内容");
        body.put("tokensUsed", 1500L);

        doNothing().when(aiService).completeTask(eq(1L), eq("生成的内容"), eq(1500L));

        mockMvc.perform(post("/api/v1/ai/task/complete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("Prompt 模板列表 - 应返回 200")
    void promptList_shouldReturn200() throws Exception {
        Map<String, Object> prompt1 = new HashMap<>();
        prompt1.put("id", 1L);
        prompt1.put("templateName", "文章生成模板");
        prompt1.put("category", "article");

        when(aiService.listPromptTemplates(eq(1L), eq("article")))
                .thenReturn(List.of(prompt1));

        mockMvc.perform(post("/api/v1/ai/prompt/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("category", "article"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].templateName").value("文章生成模板"));
    }

    @Test
    @DisplayName("新增 Prompt 模板 - 应返回 200")
    void promptSave_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateName", "新模板");
        body.put("templateContent", "请生成{topic}相关的内容");
        body.put("category", "general");
        body.put("variables", "topic");
        body.put("status", 1);

        when(aiService.savePromptTemplate(eq(1L), isNull(), eq("新模板"),
                eq("请生成{topic}相关的内容"), eq("general"), eq("topic"), eq(1)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/prompt/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("新增 Prompt 模板（缺少必填字段）- 应返回 1001")
    void promptSave_withoutRequiredFields_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("templateName", "新模板");

        mockMvc.perform(post("/api/v1/ai/prompt/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("templateName 和 templateContent 不能为空"));
    }

    @Test
    @DisplayName("删除 Prompt 模板 - 应返回 204")
    void promptDelete_shouldReturn204() throws Exception {
        doNothing().when(aiService).deletePromptTemplate(eq(1L));

        mockMvc.perform(post("/api/v1/ai/prompt/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("知识库列表 - 应返回 200")
    void knowledgeList_shouldReturn200() throws Exception {
        Map<String, Object> kb1 = new HashMap<>();
        kb1.put("id", 1L);
        kb1.put("kbName", "产品知识库");

        when(aiService.listKnowledgeBases(eq(1L)))
                .thenReturn(List.of(kb1));

        mockMvc.perform(post("/api/v1/ai/knowledge/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].kbName").value("产品知识库"));
    }

    @Test
    @DisplayName("知识库详情 - 应返回 200")
    void knowledgeGet_shouldReturn200() throws Exception {
        Map<String, Object> kb = new HashMap<>();
        kb.put("id", 1L);
        kb.put("kbName", "产品知识库");
        kb.put("description", "存储产品相关知识");

        when(aiService.getKnowledgeBaseById(eq(1L)))
                .thenReturn(kb);

        mockMvc.perform(post("/api/v1/ai/knowledge/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.kbName").value("产品知识库"));
    }

    @Test
    @DisplayName("新增知识库 - 应返回 200")
    void knowledgeSave_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("kbName", "新知识库");
        body.put("description", "测试知识库");
        body.put("embeddingModel", "text-embedding-ada-002");

        when(aiService.saveKnowledgeBase(eq(1L), isNull(), eq("新知识库"),
                eq("测试知识库"), eq("text-embedding-ada-002")))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/ai/knowledge/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("新增知识库（缺少 kbName）- 应返回 1001")
    void knowledgeSave_withoutKbName_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("description", "测试");

        mockMvc.perform(post("/api/v1/ai/knowledge/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001))
                .andExpect(jsonPath("$.message").value("kbName 不能为空"));
    }

    @Test
    @DisplayName("删除知识库 - 应返回 204")
    void knowledgeDelete_shouldReturn204() throws Exception {
        doNothing().when(aiService).deleteKnowledgeBase(eq(1L));

        mockMvc.perform(post("/api/v1/ai/knowledge/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新知识库状态 - 应返回 204")
    void knowledgeStatus_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);
        body.put("status", 1);

        doNothing().when(aiService).updateKnowledgeBaseStatus(eq(1L), eq(1));

        mockMvc.perform(post("/api/v1/ai/knowledge/status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/ai/model/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
