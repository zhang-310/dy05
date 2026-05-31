package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.port.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationPortImpl implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationPortImpl.class);

    @Override
    public void send(Notification notification) {
        log.info("[通知] {} → {} ({}): {}", notification.channel(),
                notification.targetUserId(), notification.title(), notification.content());
    }

    @Override
    public List<Notification> listPending(String tenantId, String userId) {
        return new ArrayList<>();
    }

    @Override
    public void markRead(Long notificationId) {
        // DB update
    }
}
