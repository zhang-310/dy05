package cn.gaifan.douyinOperations.module.wecom.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.wecom.service.impl.WecomServiceImpl;
import cn.gaifan.douyinOperations.module.wecom.vo.*;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("WecomController 集成测试")
class WecomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WecomServiceImpl wecomService;

    @Test
    @DisplayName("机器人列表 - 应返回 200")
    void robotList_shouldReturn200() throws Exception {
        WcRobotSearchVO searchVO = new WcRobotSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        WcRobotConfigVO robotVO = new WcRobotConfigVO();
        robotVO.setId(1L);
        robotVO.setRobotName("测试机器人");

        PageResultVO<WcRobotConfigVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(robotVO));

        when(wecomService.searchRobots(any(WcRobotSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/wecom/robot/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("机器人列表（未登录）- 应返回 2001")
    void robotList_unauthorized_shouldReturn2001() throws Exception {
        WcRobotSearchVO searchVO = new WcRobotSearchVO();

        mockMvc.perform(post("/api/v1/wecom/robot/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取机器人详情 - 应返回 200")
    void robotGet_shouldReturn200() throws Exception {
        WcRobotConfigVO robotVO = new WcRobotConfigVO();
        robotVO.setId(1L);
        robotVO.setRobotName("测试机器人");

        when(wecomService.getRobotById(eq(1L)))
                .thenReturn(robotVO);

        mockMvc.perform(post("/api/v1/wecom/robot/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取机器人详情（未登录）- 应返回 2001")
    void robotGet_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/wecom/robot/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存机器人 - 应返回 200")
    void robotSave_shouldReturn200() throws Exception {
        WcRobotConfigSaveVO saveVO = new WcRobotConfigSaveVO();
        saveVO.setOwnerId(1L);
        saveVO.setRobotName("新机器人");
        saveVO.setWebhookUrl("https://example.com/webhook");

        when(wecomService.saveRobot(any(WcRobotConfigSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/wecom/robot/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存机器人（未登录）- 应返回 1001")
    void robotSave_unauthorized_shouldReturn1001() throws Exception {
        WcRobotConfigSaveVO saveVO = new WcRobotConfigSaveVO();
        saveVO.setRobotName("新机器人");

        mockMvc.perform(post("/api/v1/wecom/robot/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除机器人 - 应返回 204")
    void robotDelete_shouldReturn204() throws Exception {
        doNothing().when(wecomService).deleteRobot(eq(1L));

        mockMvc.perform(post("/api/v1/wecom/robot/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除机器人（未登录）- 应返回 2001")
    void robotDelete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/wecom/robot/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("更新机器人状态 - 应返回 204")
    void robotStatus_shouldReturn204() throws Exception {
        doNothing().when(wecomService).updateRobotStatus(eq(1L), eq(1));

        mockMvc.perform(post("/api/v1/wecom/robot/update-status")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新机器人状态（未登录）- 应返回 2001")
    void robotStatus_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/wecom/robot/update-status")
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("推送规则列表 - 应返回 200")
    void ruleList_shouldReturn200() throws Exception {
        WcPushRuleVO ruleVO = new WcPushRuleVO();
        ruleVO.setId(1L);
        ruleVO.setRuleName("测试规则");

        when(wecomService.listRules(eq(1L)))
                .thenReturn(List.of(ruleVO));

        mockMvc.perform(post("/api/v1/wecom/rule/list")
                        .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("推送规则列表（未登录）- 应返回 2001")
    void ruleList_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/wecom/rule/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取推送规则详情 - 应返回 200")
    void ruleGet_shouldReturn200() throws Exception {
        WcPushRuleVO ruleVO = new WcPushRuleVO();
        ruleVO.setId(1L);
        ruleVO.setRuleName("测试规则");

        when(wecomService.getRuleById(eq(1L)))
                .thenReturn(ruleVO);

        mockMvc.perform(post("/api/v1/wecom/rule/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("获取推送规则详情（未登录）- 应返回 2001")
    void ruleGet_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/wecom/rule/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("保存推送规则 - 应返回 200")
    void ruleSave_shouldReturn200() throws Exception {
        WcPushRuleSaveVO saveVO = new WcPushRuleSaveVO();
        saveVO.setOwnerId(1L);
        saveVO.setRobotId(1L);
        saveVO.setRuleName("新规则");
        saveVO.setTriggerType("live_start");
        saveVO.setTriggerConfig("{}");
        saveVO.setMessageTemplate("直播开始通知");

        when(wecomService.saveRule(any(WcPushRuleSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/wecom/rule/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("保存推送规则（未登录）- 应返回 1001")
    void ruleSave_unauthorized_shouldReturn1001() throws Exception {
        WcPushRuleSaveVO saveVO = new WcPushRuleSaveVO();
        saveVO.setRuleName("新规则");

        mockMvc.perform(post("/api/v1/wecom/rule/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除推送规则 - 应返回 204")
    void ruleDelete_shouldReturn204() throws Exception {
        doNothing().when(wecomService).deleteRule(eq(1L));

        mockMvc.perform(post("/api/v1/wecom/rule/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除推送规则（未登录）- 应返回 2001")
    void ruleDelete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/wecom/rule/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("更新推送规则状态 - 应返回 204")
    void ruleStatus_shouldReturn204() throws Exception {
        doNothing().when(wecomService).updateRuleStatus(eq(1L), eq(1));

        mockMvc.perform(post("/api/v1/wecom/rule/update-status")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("更新推送规则状态（未登录）- 应返回 2001")
    void ruleStatus_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/wecom/rule/update-status")
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("消息日志列表 - 应返回 200")
    void logList_shouldReturn200() throws Exception {
        WcMessageLogSearchVO searchVO = new WcMessageLogSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        WcMessageLogVO logVO = new WcMessageLogVO();
        logVO.setId(1L);
        logVO.setMessageContent("测试消息");

        PageResultVO<WcMessageLogVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(logVO));

        when(wecomService.searchLogs(any(WcMessageLogSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/wecom/log/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("消息日志列表（未登录）- 应返回 2001")
    void logList_unauthorized_shouldReturn2001() throws Exception {
        WcMessageLogSearchVO searchVO = new WcMessageLogSearchVO();

        mockMvc.perform(post("/api/v1/wecom/log/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("手动推送消息 - 应返回 204")
    void push_shouldReturn204() throws Exception {
        WcSendMessageVO sendVO = new WcSendMessageVO();
        sendVO.setRobotId(1L);
        sendVO.setMessageContent("测试消息");

        doNothing().when(wecomService).sendMessage(any(WcSendMessageVO.class), eq(1L));

        mockMvc.perform(post("/api/v1/wecom/push")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("手动推送消息（未登录）- 应返回 2001")
    void push_unauthorized_shouldReturn2001() throws Exception {
        WcSendMessageVO sendVO = new WcSendMessageVO();
        sendVO.setRobotId(1L);
        sendVO.setMessageContent("测试消息");

        mockMvc.perform(post("/api/v1/wecom/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
