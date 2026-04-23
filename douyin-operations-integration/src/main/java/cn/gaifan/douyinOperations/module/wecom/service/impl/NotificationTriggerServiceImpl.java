package cn.gaifan.douyinOperations.module.wecom.service.impl;

import cn.gaifan.douyinOperations.module.wecom.service.NotificationTriggerService;
import cn.gaifan.douyinOperations.module.wecom.service.WecomService;
import cn.gaifan.douyinOperations.module.wecom.vo.WcSendMessageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationTriggerServiceImpl implements NotificationTriggerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTriggerServiceImpl.class);

    @Autowired(required = false)
    private WecomService wecomService;

    @Override
    public void onDailyBatchCompleted(Long ownerId, int count, String summary) {
        sendMarkdown(ownerId, "日更完成通知", String.format("### 一键日更已完成\n- 生成数量：**%d** 条\n- %s", count, summary));
    }

    @Override
    public void onLiveAlert(Long ownerId, String alertType, String message) {
        sendMarkdown(ownerId, "直播预警", String.format("### ⚠ 直播预警 [%s]\n%s", alertType, message));
    }

    @Override
    public void onApprovalResult(Long ownerId, int approved, int rejected) {
        sendMarkdown(ownerId, "审核结果通知", String.format("### 话术审核结果\n- 通过：**%d** 条\n- 驳回：**%d** 条", approved, rejected));
    }

    @Override
    public void onEffectivenessReport(Long ownerId, Long sessionId, double avgScore, int highScoreCount) {
        sendMarkdown(ownerId, "直播效果报告", String.format("### 直播效果报告\n- 平均效果分：**%.1f**\n- 高分话术：**%d** 条已入库", avgScore, highScoreCount));
    }

    @Override
    public void onEvolutionCompleted(Long ownerId, int newEntries, int updatedEntries, double qualityDelta) {
        sendMarkdown(ownerId, "知识进化通知", String.format("### 知识库进化完成\n- 新增：**%d** 条\n- 更新：**%d** 条\n- 质量提升：**+%.1f** 分", newEntries, updatedEntries, qualityDelta));
    }

    @Override
    public void onSystemAlert(String ruleName, String metricName, String severity, String message) {
        String severityLabel = "critical".equals(severity) ? "严重" : "警告";
        String color = "critical".equals(severity) ? "red" : "orange";
        String content = String.format(
                "### <font color=\"%s\">系统告警 [%s]</font>\n" +
                "- **规则名称**：%s\n" +
                "- **监控指标**：%s\n" +
                "- **告警级别**：<font color=\"%s\">%s</font>\n" +
                "- **告警详情**：%s\n" +
                "- **触发时间**：%s",
                color, severityLabel, ruleName, metricName, color, severityLabel, message,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
        // System alerts are sent without a specific owner (broadcast to admin group)
        sendMarkdown(null, "系统告警: " + ruleName, content);
    }

    private void sendMarkdown(Long ownerId, String subject, String content) {
        if (wecomService == null) {
            log.debug("企微服务未注入，跳过通知: {}", subject);
            return;
        }
        try {
            WcSendMessageVO msg = new WcSendMessageVO();
            msg.setMessageType("markdown");
            msg.setMessageContent(content);
            wecomService.sendMessage(msg, ownerId);
            log.info("企微通知已发送: ownerId={}, subject={}", ownerId, subject);
        } catch (Exception e) {
            log.warn("企微通知发送失败: ownerId={}, subject={}, error={}", ownerId, subject, e.getMessage());
        }
    }
}
