package cn.gaifan.douyinOperations.contract.port;

/**
 * 通知 Port — 跨模块消息推送
 */
public interface NotificationPort {
    record Notification(String title, String content, String channel, String targetUserId) {}

    void send(Notification notification);
    java.util.List<Notification> listPending(String tenantId, String userId);
    void markRead(Long notificationId);
}
