package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.agent.skill.SkillRegistry;
import cn.gaifan.douyinOperations.module.agent.service.SkillExecutor.ToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P0-8: AgentFunctionCallingService 单元测试（阶段 1）
 * 测试核心功能：工具定义构建、LLM 调用、工具执行
 */
@ExtendWith(MockitoExtension.class)
class AgentFunctionCallingServiceTest {

    @Mock
    private LlmClient llmClient;

    @Mock
    private SkillRegistry skillRegistry;

    @Mock
    private SkillExecutor skillExecutor;

    @Mock
    private AiModelRepository aiModelRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AgentFunctionCallingService functionCallingService;

    private AiModel testModel;
    private Long agentId = 1L;
    private Long userId = 100L;
    private Long conversationId = 200L;

    @BeforeEach
    void setUp() {
        testModel = new AiModel();
        testModel.setId(1L);
        testModel.setModelName("gpt-4");
        testModel.setIsDefault(1);
        testModel.setStatus(1);
    }

    // ==================== buildToolsJson 测试 ====================

    @Test
    void buildToolsJson_shouldReturnEmptyArray_whenNoTools() {
        // Given
        List<AgentFunctionCallingService.ToolDefinition> tools = Collections.emptyList();

        // When
        String result = functionCallingService.buildToolsJson(tools);

        // Then
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void buildToolsJson_shouldReturnValidJson_whenToolsProvided() throws Exception {
        // Given
        Map<String, Object> params = Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of(
                    "type", "string",
                    "description", "搜索关键词"
                )
            ),
            "required", List.of("query")
        );
        AgentFunctionCallingService.ToolDefinition tool =
            new AgentFunctionCallingService.ToolDefinition("kb_rag_search", "知识库搜索", params);
        List<AgentFunctionCallingService.ToolDefinition> tools = List.of(tool);

        // When
        String result = functionCallingService.buildToolsJson(tools);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).contains("kb_rag_search");
        assertThat(result).contains("知识库搜索");

        // 验证 JSON 格式正确
        List<Map<String, Object>> parsed = objectMapper.readValue(result, List.class);
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).get("type")).isEqualTo("function");
    }

    // ==================== execute 测试（无工具场景）====================

    @Test
    void execute_shouldReturnFallback_whenNoToolsAvailable() {
        // Given
        String systemPrompt = "你是智能助手";
        String userMessage = "你好";

        when(skillExecutor.getAvailableToolNames(agentId)).thenReturn(Collections.emptyList());
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(testModel));

        LlmClient.LlmResponse mockResponse = new LlmClient.LlmResponse(
            "你好！有什么可以帮助你的吗？", 100, true, null
        );
        when(llmClient.chat(any(AiModel.class), anyString(), anyString())).thenReturn(mockResponse);

        // When
        AgentFunctionCallingService.FunctionCallingResult result =
            functionCallingService.execute(agentId, userId, conversationId, systemPrompt, userMessage, null);

        // Then
        assertThat(result.success).isTrue();
        assertThat(result.content).contains("你好");
        assertThat(result.toolCalls).isEmpty();
        verify(llmClient).chat(any(AiModel.class), anyString(), anyString());
    }

    // ==================== execute 测试（有工具但无调用）====================

    @Test
    void execute_shouldReturnTextResponse_whenNoToolCallsInResponse() {
        // Given
        String systemPrompt = "你是智能助手";
        String userMessage = "你好";

        when(skillExecutor.getAvailableToolNames(agentId)).thenReturn(List.of("kb_rag_search"));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(testModel));

        Skill mockSkill = mock(Skill.class);
        when(mockSkill.getName()).thenReturn("kb_rag_search");
        when(mockSkill.getDescription()).thenReturn("知识库搜索");
        when(skillRegistry.findByName("kb_rag_search")).thenReturn(Optional.of(mockSkill));

        LlmClient.LlmToolResponse mockResponse = new LlmClient.LlmToolResponse(
            "你好！有什么可以帮助你的吗？", 100, true, null, null, null
        );
        when(llmClient.chatWithToolsStructured(any(AiModel.class), anyList(), anyString()))
            .thenReturn(mockResponse);

        // When
        AgentFunctionCallingService.FunctionCallingResult result =
            functionCallingService.execute(agentId, userId, conversationId, systemPrompt, userMessage, null);

        // Then
        assertThat(result.success).isTrue();
        assertThat(result.content).contains("你好");
        assertThat(result.toolCalls).isEmpty();
    }

    // ==================== execute 测试（有工具调用）====================

    @Test
    void execute_shouldExecuteTool_whenToolCallsInResponse() throws Exception {
        // Given
        String systemPrompt = "你是智能助手";
        String userMessage = "搜索产品信息";

        when(skillExecutor.getAvailableToolNames(agentId)).thenReturn(List.of("kb_rag_search"));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(testModel));

        Skill mockSkill = mock(Skill.class);
        when(mockSkill.getName()).thenReturn("kb_rag_search");
        when(mockSkill.getDescription()).thenReturn("知识库搜索");
        when(mockSkill.execute(any(Skill.SkillContext.class))).thenReturn("搜索结果：找到3条相关文档");
        when(skillRegistry.findByName("kb_rag_search")).thenReturn(Optional.of(mockSkill));

        // 第一轮：返回工具调用
        String toolCallsJson = "[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"kb_rag_search\",\"arguments\":\"{\\\"query\\\":\\\"产品信息\\\"}\"}}]";
        LlmClient.LlmToolResponse firstResponse = new LlmClient.LlmToolResponse(
            "", 100, true, null, toolCallsJson, null
        );

        // 第二轮：返回最终文本
        LlmClient.LlmToolResponse secondResponse = new LlmClient.LlmToolResponse(
            "根据搜索结果，找到了3条相关文档", 100, true, null, null, null
        );

        when(llmClient.chatWithToolsStructured(any(AiModel.class), anyList(), anyString()))
            .thenReturn(firstResponse, secondResponse);

        // When
        AgentFunctionCallingService.FunctionCallingResult result =
            functionCallingService.execute(agentId, userId, conversationId, systemPrompt, userMessage, null);

        // Then
        assertThat(result.success).isTrue();
        assertThat(result.content).contains("根据搜索结果");
        assertThat(result.toolCalls).hasSize(1);
        assertThat(result.toolCalls.get(0).toolName).isEqualTo("kb_rag_search");
        assertThat(result.toolCalls.get(0).success).isTrue();
        verify(mockSkill).execute(any(Skill.SkillContext.class));
    }

    // ==================== execute 测试（工具执行失败）====================

    @Test
    void execute_shouldHandleToolExecutionError() throws Exception {
        // Given
        String systemPrompt = "你是智能助手";
        String userMessage = "搜索产品信息";

        when(skillExecutor.getAvailableToolNames(agentId)).thenReturn(List.of("kb_rag_search"));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(testModel));

        Skill mockSkill = mock(Skill.class);
        when(mockSkill.getName()).thenReturn("kb_rag_search");
        when(mockSkill.getDescription()).thenReturn("知识库搜索");
        when(mockSkill.execute(any(Skill.SkillContext.class)))
            .thenThrow(new RuntimeException("知识库连接失败"));
        when(skillRegistry.findByName("kb_rag_search")).thenReturn(Optional.of(mockSkill));

        // 第一轮：返回工具调用
        String toolCallsJson = "[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"kb_rag_search\",\"arguments\":\"{\\\"query\\\":\\\"产品信息\\\"}\"}}]";
        LlmClient.LlmToolResponse firstResponse = new LlmClient.LlmToolResponse(
            "", 100, true, null, toolCallsJson, null
        );

        // 第二轮：返回最终文本
        LlmClient.LlmToolResponse secondResponse = new LlmClient.LlmToolResponse(
            "抱歉，搜索时遇到了问题", 100, true, null, null, null
        );

        when(llmClient.chatWithToolsStructured(any(AiModel.class), anyList(), anyString()))
            .thenReturn(firstResponse, secondResponse);

        // When
        AgentFunctionCallingService.FunctionCallingResult result =
            functionCallingService.execute(agentId, userId, conversationId, systemPrompt, userMessage, null);

        // Then
        assertThat(result.success).isTrue();
        assertThat(result.toolCalls).hasSize(1);
        assertThat(result.toolCalls.get(0).success).isFalse();
        assertThat(result.toolCalls.get(0).error).contains("知识库连接失败");
    }

    // ==================== execute 测试（工具不存在）====================

    @Test
    void execute_shouldHandleMissingTool() throws Exception {
        // Given
        String systemPrompt = "你是智能助手";
        String userMessage = "搜索产品信息";

        when(skillExecutor.getAvailableToolNames(agentId)).thenReturn(List.of("kb_rag_search"));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(testModel));

        // 工具不存在 - buildToolDefinitions() 会返回空列表，触发降级为普通对话
        when(skillRegistry.findByName("kb_rag_search")).thenReturn(Optional.empty());

        // 降级为普通对话，使用 llmClient.chat()
        LlmClient.LlmResponse fallbackResponse = new LlmClient.LlmResponse(
            "抱歉，工具不可用", 100, true, null
        );
        when(llmClient.chat(any(AiModel.class), anyString(), anyString())).thenReturn(fallbackResponse);

        // When
        AgentFunctionCallingService.FunctionCallingResult result =
            functionCallingService.execute(agentId, userId, conversationId, systemPrompt, userMessage, null);

        // Then
        assertThat(result.success).isTrue(); // 整体执行成功（LLM 返回了回复）
        assertThat(result.toolCalls).isEmpty(); // 降级模式下无工具调用
        assertThat(result.content).contains("抱歉");
    }

    // ==================== execute 测试（LLM 调用失败）====================

    @Test
    void execute_shouldReturnFallback_whenLlmCallFails() {
        // Given
        String systemPrompt = "你是智能助手";
        String userMessage = "你好";

        when(skillExecutor.getAvailableToolNames(agentId)).thenReturn(List.of("kb_rag_search"));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(testModel));

        Skill mockSkill = mock(Skill.class);
        when(mockSkill.getName()).thenReturn("kb_rag_search");
        when(mockSkill.getDescription()).thenReturn("知识库搜索");
        when(skillRegistry.findByName("kb_rag_search")).thenReturn(Optional.of(mockSkill));

        LlmClient.LlmToolResponse errorResponse = new LlmClient.LlmToolResponse(
            null, 0, false, "API 调用失败", null, null
        );
        when(llmClient.chatWithToolsStructured(any(AiModel.class), anyList(), anyString()))
            .thenReturn(errorResponse);

        // When
        AgentFunctionCallingService.FunctionCallingResult result =
            functionCallingService.execute(agentId, userId, conversationId, systemPrompt, userMessage, null);

        // Then
        assertThat(result.success).isFalse();
        assertThat(result.content).contains("AI 服务调用失败");
        assertThat(result.error).contains("API 调用失败");
    }

    // ==================== execute 测试（达到最大轮次）====================

    @Test
    void execute_shouldStopAtMaxRounds() throws Exception {
        // Given
        String systemPrompt = "你是智能助手";
        String userMessage = "搜索产品信息";

        when(skillExecutor.getAvailableToolNames(agentId)).thenReturn(List.of("kb_rag_search"));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(testModel));

        Skill mockSkill = mock(Skill.class);
        when(mockSkill.getName()).thenReturn("kb_rag_search");
        when(mockSkill.getDescription()).thenReturn("知识库搜索");
        when(mockSkill.execute(any(Skill.SkillContext.class))).thenReturn("搜索结果");
        when(skillRegistry.findByName("kb_rag_search")).thenReturn(Optional.of(mockSkill));

        // 每轮都返回工具调用（模拟无限循环）
        String toolCallsJson = "[{\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"kb_rag_search\",\"arguments\":\"{\\\"query\\\":\\\"产品信息\\\"}\"}}]";
        LlmClient.LlmToolResponse toolCallResponse = new LlmClient.LlmToolResponse(
            "", 100, true, null, toolCallsJson, null
        );

        // 最后一轮返回文本（达到最大轮次后的最终调用）
        LlmClient.LlmToolResponse finalResponse = new LlmClient.LlmToolResponse(
            "已达到最大轮次", 100, true, null, null, null
        );

        when(llmClient.chatWithToolsStructured(any(AiModel.class), anyList(), anyString()))
            .thenReturn(toolCallResponse, toolCallResponse, toolCallResponse, finalResponse);

        // When
        AgentFunctionCallingService.FunctionCallingResult result =
            functionCallingService.execute(agentId, userId, conversationId, systemPrompt, userMessage, null);

        // Then
        assertThat(result.success).isTrue();
        // 注意：result.toolCalls 是 FunctionCallingResult 中记录的工具调用历史
        // 但达到最大轮次后，callLlmFinalResponse 返回的是空列表
        // 实际的工具调用记录在 allToolCalls 中，但不会传递到最终结果
        // 所以这里应该检查 LLM 被调用了 4 次（3次工具调用 + 1次最终回复）
        verify(llmClient, times(4)).chatWithToolsStructured(any(AiModel.class), anyList(), anyString());
        verify(mockSkill, times(3)).execute(any(Skill.SkillContext.class)); // 工具被执行了3次
    }
}
