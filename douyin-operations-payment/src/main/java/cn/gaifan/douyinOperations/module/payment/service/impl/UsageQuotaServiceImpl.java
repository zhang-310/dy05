package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.tenant.TenantOrgResolutionHelper;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class UsageQuotaServiceImpl implements UsageQuotaService {

    @Autowired
    private UsageRecordRepository usageRecordRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private TenantOrgResolutionHelper tenantOrgResolutionHelper;

    @Override
    public void recordUsage(Long userId, String metric, int delta) {
        UsageRecord record = new UsageRecord();
        record.setUserId(userId);
        record.setOrgId(tenantOrgResolutionHelper.organizationIdForUser(userId));
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

    @Override
    public Map<String, Object> getUsageQuotaSummary(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aiCall", quotaItem(userId, "aiGenerations", "AI 调用额度"));
        result.put("liveSession", quotaItem(userId, "liveSessions", "直播场次"));
        result.put("svProject", quotaItem(userId, "svProjects", "短视频项目"));
        result.put("storage", quotaItem(userId, "storageMb", "存储空间"));

        Map<String, Object> ai = castMap(result.get("aiCall"));
        Map<String, Object> live = castMap(result.get("liveSession"));
        Map<String, Object> storage = castMap(result.get("storage"));
        result.put("aiCallUsed", ai.get("used"));
        result.put("aiCallLimit", ai.get("total"));
        result.put("aiCallLast7d", getUsageSince(userId, "aiGenerations", LocalDate.now().minusDays(7)));
        result.put("liveSessionUsed", live.get("used"));
        result.put("liveSessionLimit", live.get("total"));
        result.put("storageUsed", storage.get("used"));
        result.put("storageLimit", storage.get("total"));
        result.put("resetTime", "按自然月");
        result.put("source", "payment_usage_record");
        return result;
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

    private Map<String, Object> quotaItem(Long userId, String metric, String label) {
        Map<String, Object> check = subscriptionService.checkQuota(userId, metric);
        int used = getCurrentMonthUsage(userId, metric);
        int total = numberValue(check.get("limit"));
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("metric", metric);
        item.put("used", used);
        item.put("total", total);
        item.put("unlimited", Boolean.TRUE.equals(check.get("unlimited")) || total < 0);
        item.put("allowed", Boolean.TRUE.equals(check.get("allowed")) || total < 0 || used < total);
        item.put("resetTime", "按自然月");
        return item;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    private int getUsageSince(Long userId, String metric, LocalDate sinceDate) {
        Timestamp since = Timestamp.from(sinceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return usageRecordRepository.sumUsageSince(userId, metric, since);
    }

    private int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
