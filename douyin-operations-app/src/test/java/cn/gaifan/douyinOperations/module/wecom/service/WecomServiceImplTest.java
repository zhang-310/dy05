package cn.gaifan.douyinOperations.module.wecom.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.wecom.entity.*;
import cn.gaifan.douyinOperations.module.wecom.repository.*;
import cn.gaifan.douyinOperations.module.wecom.service.impl.WecomServiceImpl;
import cn.gaifan.douyinOperations.module.wecom.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WecomService 单元测试")
class WecomServiceImplTest {

    @Mock
    private WcRobotConfigRepository robotConfigRepository;
    @Mock
    private WcPushRuleRepository pushRuleRepository;
    @Mock
    private WcMessageLogRepository messageLogRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WecomServiceImpl wecomService;

    private Long ownerId = 1L;
    private WcRobotConfig mockRobot;
    private WcPushRule mockRule;
    private WcMessageLog mockLog;

    @BeforeEach
    void setUp() {
        mockRobot = new WcRobotConfig();
        mockRobot.setId(1L);
        mockRobot.setOwnerId(ownerId);
        mockRobot.setRobotName("测试机器人");
        mockRobot.setWebhookUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test");
        mockRobot.setRobotType("custom");
        mockRobot.setStatus(1);
        mockRobot.setDescription("测试描述");
        mockRobot.setDeleted(0);
        mockRobot.setCreateTime(new Timestamp(System.currentTimeMillis()));
        mockRobot.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        mockRule = new WcPushRule();
        mockRule.setId(1L);
        mockRule.setOwnerId(ownerId);
        mockRule.setRobotId(1L);
        mockRule.setRuleName("测试规则");
        mockRule.setTriggerType("schedule");
        mockRule.setTriggerConfig("{\"cron\":\"0 9 * * *\"}");
        mockRule.setMessageTemplate("每日报告");
        mockRule.setStatus(1);
        mockRule.setDeleted(0);

        mockLog = new WcMessageLog();
        mockLog.setId(1L);
        mockLog.setOwnerId(ownerId);
        mockLog.setRobotId(1L);
        mockLog.setRuleId(1L);
        mockLog.setMessageType("text");
        mockLog.setMessageContent("测试消息");
        mockLog.setStatus(1);
        mockLog.setSendTime(new Timestamp(System.currentTimeMillis()));
    }

    // ==================== 机器人管理测试 ====================

    @Test
    @DisplayName("搜索机器人 - 应返回分页结果")
    void searchRobots_shouldReturnPageResult() {
        // Given
        WcRobotSearchVO searchVO = new WcRobotSearchVO();
        searchVO.setOwnerId(ownerId);
        Page<WcRobotConfig> page = new PageImpl<>(List.of(mockRobot));
        when(robotConfigRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<WcRobotConfigVO> result = wecomService.searchRobots(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getRobotName()).isEqualTo("测试机器人");
    }

    @Test
    @DisplayName("获取机器人详情 - 正常情况")
    void getRobotById_normal_shouldReturnVO() {
        // Given
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));

        // When
        WcRobotConfigVO result = wecomService.getRobotById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRobotName()).isEqualTo("测试机器人");
    }

    @Test
    @DisplayName("获取机器人详情 - 不存在应抛出异常")
    void getRobotById_notFound_shouldThrowException() {
        // Given
        when(robotConfigRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> wecomService.getRobotById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("机器人配置不存在");
    }

    @Test
    @DisplayName("保存机器人 - 新建应返回ID")
    void saveRobot_create_shouldReturnId() {
        // Given
        WcRobotConfigSaveVO saveVO = new WcRobotConfigSaveVO();
        saveVO.setOwnerId(ownerId);
        saveVO.setRobotName("新机器人");
        saveVO.setWebhookUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=new");
        saveVO.setRobotType("alert");
        when(robotConfigRepository.save(any(WcRobotConfig.class)))
                .thenReturn(mockRobot);

        // When
        long result = wecomService.saveRobot(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(robotConfigRepository).save(argThat(robot ->
                robot.getOwnerId().equals(ownerId) &&
                robot.getRobotName().equals("新机器人")
        ));
    }

    @Test
    @DisplayName("保存机器人 - 更新应修改现有记录")
    void saveRobot_update_shouldModifyExisting() {
        // Given
        WcRobotConfigSaveVO saveVO = new WcRobotConfigSaveVO();
        saveVO.setId(1L);
        saveVO.setOwnerId(ownerId);
        saveVO.setRobotName("更新后的机器人");
        saveVO.setWebhookUrl("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=updated");
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));
        when(robotConfigRepository.save(any(WcRobotConfig.class)))
                .thenReturn(mockRobot);

        // When
        long result = wecomService.saveRobot(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(robotConfigRepository).save(argThat(robot ->
                robot.getRobotName().equals("更新后的机器人")
        ));
    }

    @Test
    @DisplayName("删除机器人 - 正常情况应逻辑删除")
    void deleteRobot_normal_shouldLogicalDelete() {
        // Given
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));
        when(robotConfigRepository.save(any(WcRobotConfig.class)))
                .thenReturn(mockRobot);

        // When
        wecomService.deleteRobot(1L);

        // Then
        verify(robotConfigRepository).save(argThat(robot -> robot.getDeleted() == 1));
    }

    @Test
    @DisplayName("更新机器人状态 - 正常情况")
    void updateRobotStatus_normal_shouldUpdate() {
        // Given
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));

        // When
        wecomService.updateRobotStatus(1L, 0);

        // Then
        verify(robotConfigRepository).updateStatus(1L, 0);
    }

    // ==================== 推送规则测试 ====================

    @Test
    @DisplayName("列出推送规则 - 应返回规则列表")
    void listRules_shouldReturnList() {
        // Given
        when(pushRuleRepository.findByOwnerIdAndDeleted(ownerId, 0))
                .thenReturn(List.of(mockRule));

        // When
        List<WcPushRuleVO> result = wecomService.listRules(ownerId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRuleName()).isEqualTo("测试规则");
    }

    @Test
    @DisplayName("获取规则详情 - 正常情况")
    void getRuleById_normal_shouldReturnVO() {
        // Given
        when(pushRuleRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRule));

        // When
        WcPushRuleVO result = wecomService.getRuleById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRuleName()).isEqualTo("测试规则");
    }

    @Test
    @DisplayName("保存规则 - 新建应返回ID")
    void saveRule_create_shouldReturnId() {
        // Given
        WcPushRuleSaveVO saveVO = new WcPushRuleSaveVO();
        saveVO.setOwnerId(ownerId);
        saveVO.setRobotId(1L);
        saveVO.setRuleName("新规则");
        saveVO.setTriggerType("event");
        saveVO.setTriggerConfig("{}");
        saveVO.setMessageTemplate("模板");
        when(pushRuleRepository.save(any(WcPushRule.class)))
                .thenReturn(mockRule);

        // When
        long result = wecomService.saveRule(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(pushRuleRepository).save(argThat(rule ->
                rule.getRuleName().equals("新规则")
        ));
    }

    @Test
    @DisplayName("删除规则 - 正常情况应逻辑删除")
    void deleteRule_normal_shouldLogicalDelete() {
        // Given
        when(pushRuleRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRule));
        when(pushRuleRepository.save(any(WcPushRule.class)))
                .thenReturn(mockRule);

        // When
        wecomService.deleteRule(1L);

        // Then
        verify(pushRuleRepository).save(argThat(rule -> rule.getDeleted() == 1));
    }

    // ==================== 消息日志测试 ====================

    @Test
    @DisplayName("搜索消息日志 - 应返回分页结果")
    void searchLogs_shouldReturnPageResult() {
        // Given
        WcMessageLogSearchVO searchVO = new WcMessageLogSearchVO();
        searchVO.setOwnerId(ownerId);
        Page<WcMessageLog> page = new PageImpl<>(List.of(mockLog));
        when(messageLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<WcMessageLogVO> result = wecomService.searchLogs(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
    }

    // ==================== 发送消息测试 ====================

    @Test
    @DisplayName("发送消息 - 成功应记录日志")
    void sendMessage_success_shouldLogSuccess() {
        // Given
        WcSendMessageVO sendVO = new WcSendMessageVO();
        sendVO.setRobotId(1L);
        sendVO.setMessageType("text");
        sendVO.setMessageContent("测试消息");
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"errcode\":0}", HttpStatus.OK));
        when(messageLogRepository.save(any(WcMessageLog.class)))
                .thenReturn(mockLog);

        // When
        wecomService.sendMessage(sendVO, ownerId);

        // Then
        verify(messageLogRepository).save(argThat(log ->
                log.getStatus() == 1 &&
                log.getMessageContent().equals("测试消息")
        ));
    }

    @Test
    @DisplayName("发送消息 - 机器人不存在应抛出异常")
    void sendMessage_robotNotFound_shouldThrowException() {
        // Given
        WcSendMessageVO sendVO = new WcSendMessageVO();
        sendVO.setRobotId(999L);
        when(robotConfigRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> wecomService.sendMessage(sendVO, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("机器人配置不存在");
    }

    @Test
    @DisplayName("发送消息 - 机器人已禁用应抛出异常")
    void sendMessage_robotDisabled_shouldThrowException() {
        // Given
        mockRobot.setStatus(0);
        WcSendMessageVO sendVO = new WcSendMessageVO();
        sendVO.setRobotId(1L);
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));

        // When & Then
        assertThatThrownBy(() -> wecomService.sendMessage(sendVO, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("机器人已禁用");
    }

    @Test
    @DisplayName("发送消息 - HTTP 失败应记录错误日志")
    void sendMessage_httpError_shouldLogError() {
        // Given
        WcSendMessageVO sendVO = new WcSendMessageVO();
        sendVO.setRobotId(1L);
        sendVO.setMessageContent("测试消息");
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        when(messageLogRepository.save(any(WcMessageLog.class)))
                .thenReturn(mockLog);

        // When
        wecomService.sendMessage(sendVO, ownerId);

        // Then
        verify(messageLogRepository).save(argThat(log ->
                log.getStatus() == 0 &&
                log.getErrorMessage() != null
        ));
    }

    @Test
    @DisplayName("发送消息 - 异常应记录错误日志")
    void sendMessage_exception_shouldLogError() {
        // Given
        WcSendMessageVO sendVO = new WcSendMessageVO();
        sendVO.setRobotId(1L);
        sendVO.setMessageContent("测试消息");
        when(robotConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockRobot));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("网络错误"));
        when(messageLogRepository.save(any(WcMessageLog.class)))
                .thenReturn(mockLog);

        // When
        wecomService.sendMessage(sendVO, ownerId);

        // Then
        verify(messageLogRepository).save(argThat(log ->
                log.getStatus() == 0 &&
                log.getErrorMessage().contains("网络错误")
        ));
    }
}
