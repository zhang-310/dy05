package cn.gaifan.douyinOperations.module.agent.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.service.impl.AgentServiceImpl;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentSearchVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AgentController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AgentController 集成测试")
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentServiceImpl agentService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @BeforeEach
    void setUp() {
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("智能体列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        AgentSearchVO searchVO = new AgentSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setAgentName("客服助手");

        AgentVO agentVO = new AgentVO();
        agentVO.setId(1L);
        agentVO.setAgentName("客服助手");
        agentVO.setAgentType(3);
        agentVO.setSystemPrompt("你是一个专业的客服助手");
        agentVO.setModelConfig("{\"model\":\"gpt-4\",\"temperature\":0.7}");
        agentVO.setResponseMode(1);
        agentVO.setDescription("智能客服助手");
        agentVO.setVersion(1);
        agentVO.setStatus(1);
        agentVO.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        agentVO.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        PageResultVO<AgentVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(agentVO));

        when(agentService.searchAgents(eq(1L), any(AgentSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/agent/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.list[0].agentName").value("客服助手"))
                .andExpect(jsonPath("$.data.list[0].agentType").value(3));
    }

    @Test
    @DisplayName("获取智能体详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        AgentVO agentVO = new AgentVO();
        agentVO.setId(1L);
        agentVO.setAgentName("客服助手");
        agentVO.setAgentType(3);
        agentVO.setSystemPrompt("你是一个专业的客服助手");

        when(agentService.getAgentById(1L)).thenReturn(agentVO);

        mockMvc.perform(post("/api/v1/agent/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.agentName").value("客服助手"));
    }

    @Test
    @DisplayName("新建智能体 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        AgentSaveVO saveVO = new AgentSaveVO();
        saveVO.setAgentName("新智能体");
        saveVO.setAgentType(1);
        saveVO.setSystemPrompt("你是一个助手");
        saveVO.setModelConfig("{\"model\":\"gpt-4\"}");
        saveVO.setResponseMode(1);
        saveVO.setDescription("测试智能体");

        when(agentService.saveAgent(eq(1L), any(AgentSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/agent/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除智能体 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        doNothing().when(agentService).deleteAgent(1L);

        mockMvc.perform(post("/api/v1/agent/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新智能体状态 - 应返回 204")
    void updateStatus_shouldReturn204() throws Exception {
        doNothing().when(agentService).updateAgentStatus(1L, 1);

        mockMvc.perform(post("/api/v1/agent/update-status")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "1")
                        .param("status", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("创建会话 - 应返回 200")
    void createConversation_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("agentId", 1L);
        body.put("topic", "新会话");

        when(agentService.createConversation(eq(1L), eq(1L), eq("新会话"))).thenReturn(100L);

        mockMvc.perform(post("/api/v1/agent/conversation/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @DisplayName("会话列表 - 应返回 200")
    void listConversations_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("agentId", 1L);

        Map<String, Object> conv1 = new HashMap<>();
        conv1.put("conversationId", 100L);
        conv1.put("title", "会话1");

        Map<String, Object> conv2 = new HashMap<>();
        conv2.put("conversationId", 101L);
        conv2.put("title", "会话2");

        when(agentService.listConversations(1L, 1L)).thenReturn(List.of(conv1, conv2));

        mockMvc.perform(post("/api/v1/agent/conversation/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("会话1"));
    }

    @Test
    @DisplayName("删除会话 - 应返回 204")
    void deleteConversation_shouldReturn204() throws Exception {
        doNothing().when(agentService).deleteConversation(100L);

        mockMvc.perform(post("/api/v1/agent/conversation/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .param("id", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("发送消息 - 应返回 200")
    void sendMessage_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", 100L);
        body.put("senderType", 0);
        body.put("content", "你好");
        body.put("tokens", 0);

        when(agentService.sendMessage(eq(100L), eq(0), eq("你好"), eq(0))).thenReturn(1000L);

        mockMvc.perform(post("/api/v1/agent/message/send")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1000));
    }

    @Test
    @DisplayName("消息列表 - 应返回 200")
    void listMessages_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", 100L);

        Map<String, Object> msg1 = new HashMap<>();
        msg1.put("messageId", 1000L);
        msg1.put("content", "你好");
        msg1.put("role", "user");

        Map<String, Object> msg2 = new HashMap<>();
        msg2.put("messageId", 1001L);
        msg2.put("content", "你好！");
        msg2.put("role", "assistant");

        when(agentService.listMessages(100L)).thenReturn(List.of(msg1, msg2));

        mockMvc.perform(post("/api/v1/agent/message/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("user"))
                .andExpect(jsonPath("$.data[1].role").value("assistant"));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        AgentSearchVO searchVO = new AgentSearchVO();

        mockMvc.perform(post("/api/v1/agent/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
