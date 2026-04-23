package cn.gaifan.douyinOperations.module.wecom.service;

import cn.gaifan.douyinOperations.module.wecom.service.impl.NotificationTriggerServiceImpl;
import cn.gaifan.douyinOperations.module.wecom.vo.WcSendMessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationTriggerService 单元测试")
class NotificationTriggerServiceImplTest {

    @Mock
    private WecomService wecomService;

    @InjectMocks
    private NotificationTriggerServiceImpl notificationTriggerService;

    private Long ownerId = 1L;

    @BeforeEach
    void setUp() {
        // Mock wecomService 已通过 @Mock 注入
    }

    @Test
    @DisplayName("日更完成通知 - 应发送 markdown 消息")
    void onDailyBatchCompleted_shouldSendMarkdown() {
        // When
        notificationTriggerService.onDailyBatchCompleted(ownerId, 10, "全部成功");

        // Then
        verify(wecomService).sendMessage(argThat(msg ->
                msg.getMessageType().equals("markdown") &&
                msg.getMessageContent().contains("一键日更已完成") &&
                msg.getMessageContent().contains("10")
        ), eq(ownerId));
    }

    @Test
    @DisplayName("直播预警通知 - 应发送 markdown 消息")
    void onLiveAlert_shouldSendMarkdown() {
        // When
        notificationTriggerService.onLiveAlert(ownerId, "低转化率", "当前转化率低于阈值");

        // Then
        verify(wecomService).sendMessage(argThat(msg ->
                msg.getMessageType().equals("markdown") &&
                msg.getMessageContent().contains("直播预警") &&
                msg.getMessageContent().contains("低转化率")
        ), eq(ownerId));
    }

    @Test
    @DisplayName("审核结果通知 - 应发送 markdown 消息")
    void onApprovalResult_shouldSendMarkdown() {
        // When
        notificationTriggerService.onApprovalResult(ownerId, 8, 2);

        // Then
        verify(wecomService).sendMessage(argThat(msg ->
                msg.getMessageType().equals("markdown") &&
                msg.getMessageContent().contains("话术审核结果") &&
                msg.getMessageContent().contains("8") &&
                msg.getMessageContent().contains("2")
        ), eq(ownerId));
    }

    @Test
    @DisplayName("直播效果报告 - 应发送 markdown 消息")
    void onEffectivenessReport_shouldSendMarkdown() {
        // When
        notificationTriggerService.onEffectivenessReport(ownerId, 100L, 8.5, 5);

        // Then
        verify(wecomService).sendMessage(argThat(msg ->
                msg.getMessageType().equals("markdown") &&
                msg.getMessageContent().contains("直播效果报告") &&
                msg.getMessageContent().contains("8.5") &&
                msg.getMessageContent().contains("5")
        ), eq(ownerId));
    }

    @Test
    @DisplayName("知识进化通知 - 应发送 markdown 消息")
    void onEvolutionCompleted_shouldSendMarkdown() {
        // When
        notificationTriggerService.onEvolutionCompleted(ownerId, 15, 8, 2.3);

        // Then
        verify(wecomService).sendMessage(argThat(msg ->
                msg.getMessageType().equals("markdown") &&
                msg.getMessageContent().contains("知识库进化完成") &&
                msg.getMessageContent().contains("15") &&
                msg.getMessageContent().contains("8") &&
                msg.getMessageContent().contains("2.3")
        ), eq(ownerId));
    }

    @Test
    @DisplayName("系统告警 - 严重级别应发送红色告警")
    void onSystemAlert_critical_shouldSendRedAlert() {
        // When
        notificationTriggerService.onSystemAlert("CPU 使用率", "cpu_usage", "critical", "CPU 使用率超过 90%");

        // Then
        verify(wecomService).sendMessage(argThat(msg ->
                msg.getMessageType().equals("markdown") &&
                msg.getMessageContent().contains("系统告警") &&
                msg.getMessageContent().contains("CPU 使用率") &&
                msg.getMessageContent().contains("严重") &&
                msg.getMessageContent().contains("red")
        ), isNull());
    }

    @Test
    @DisplayName("系统告警 - 警告级别应发送橙色告警")
    void onSystemAlert_warning_shouldSendOrangeAlert() {
        // When
        notificationTriggerService.onSystemAlert("内存使用率", "memory_usage", "warning", "内存使用率超过 80%");

        // Then
        verify(wecomService).sendMessage(argThat(msg ->
                msg.getMessageType().equals("markdown") &&
                msg.getMessageContent().contains("系统告警") &&
                msg.getMessageContent().contains("内存使用率") &&
                msg.getMessageContent().contains("警告") &&
                msg.getMessageContent().contains("orange")
        ), isNull());
    }

    @Test
    @DisplayName("发送通知 - wecomService 抛出异常应捕获")
    void sendNotification_wecomServiceThrows_shouldCatchException() {
        // Given
        doThrow(new RuntimeException("网络错误"))
                .when(wecomService).sendMessage(any(WcSendMessageVO.class), anyLong());

        // When & Then - 不应抛出异常
        notificationTriggerService.onDailyBatchCompleted(ownerId, 10, "测试");

        verify(wecomService).sendMessage(any(WcSendMessageVO.class), eq(ownerId));
    }
}
