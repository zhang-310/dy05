package cn.gaifan.douyinOperations.module.wecom.service;

public interface NotificationTriggerService {
    void onDailyBatchCompleted(Long ownerId, int count, String summary);
    void onLiveAlert(Long ownerId, String alertType, String message);
    void onApprovalResult(Long ownerId, int approved, int rejected);
    void onEffectivenessReport(Long ownerId, Long sessionId, double avgScore, int highScoreCount);
    void onEvolutionCompleted(Long ownerId, int newEntries, int updatedEntries, double qualityDelta);

    /**
     * 系统告警触发通知（由 AlertEngineService 调用）
     * @param ruleName   规则名称
     * @param metricName 指标名称
     * @param severity   告警级别 (warning/critical)
     * @param message    告警详情
     */
    void onSystemAlert(String ruleName, String metricName, String severity, String message);
}
