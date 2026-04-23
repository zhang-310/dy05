package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.entity.UsageRecord;
import cn.gaifan.douyinOperations.module.payment.repository.UsageRecordRepository;
import cn.gaifan.douyinOperations.module.payment.service.SubscriptionService;
import cn.gaifan.douyinOperations.module.payment.service.UsageQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
public class UsageQuotaServiceImpl implements UsageQuotaService {

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public void recordUsage(Long userId, String metric, int delta) {
        UsageRecord record = new UsageRecord();
        record.setUserId(userId);
        record.setMetric(metric);
        record.setDelta(delta);
        usageRecordRepository.save(record);
        log.debug("[UsageQuota] 记录用量: userId={}, metric={}, delta={}", userId, metric, delta);
    }

    @Override
    public boolean isWithinQuota(Long userId, String metric) {
        Subscription sub = subscriptionService.getActiveSubscription(userId);
        int limit = getLimit(sub, metric);
        if (limit == -1) return true; // unlimited

        int currentUsage = getCurrentMonthUsage(userId, metric);
        return currentUsage < limit;
    }

    @Override
    public int getCurrentMonthUsage(Long userId, String metric) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        Timestamp since = Timestamp.from(firstOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return usageRecordRepository.sumUsageSince(userId, metric, since);
    }

    private int getLimit(Subscription sub, String metric) {
        if (sub == null) {
            // Default free limits
            return switch (metric) {
                case "liveSessions" -> 5;
                case "svProjects" -> 10;
                case "aiGenerations" -> 50;
                default -> 100;
            };
        }
        return switch (metric) {
            case "liveSessions" -> sub.getMaxLiveSessions();
            case "svProjects" -> sub.getMaxSvProjects();
            case "aiGenerations" -> sub.getMaxAiGenerations();
            case "storageMb" -> sub.getMaxStorageMb();
            default -> -1;
        };
    }
}
