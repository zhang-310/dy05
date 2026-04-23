package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.repository.SubscriptionRepository;
import cn.gaifan.douyinOperations.module.payment.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private static final Map<String, Map<String, Integer>> PLAN_LIMITS = Map.of(
            "free", Map.of("maxLiveSessions", 5, "maxSvProjects", 10, "maxAiGenerations", 50, "maxStorageMb", 500),
            "pro", Map.of("maxLiveSessions", 50, "maxSvProjects", 100, "maxAiGenerations", -1, "maxStorageMb", 5000),
            "enterprise", Map.of("maxLiveSessions", -1, "maxSvProjects", -1, "maxAiGenerations", -1, "maxStorageMb", -1)
    );

    @Override
    public Subscription getActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndDeletedAndStatus(userId, 0, "active")
                .filter(s -> !s.isExpired())
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Subscription createOrUpgrade(Long userId, String plan) {
        var existingOpt = subscriptionRepository.findByUserIdAndDeletedAndStatus(userId, 0, "active");

        Subscription sub;
        if (existingOpt.isPresent()) {
            sub = existingOpt.get();
        } else {
            sub = new Subscription();
            sub.setUserId(userId);
        }

        sub.setPlan(plan);
        sub.setStatus("active");
        sub.setStartedAt(new Timestamp(System.currentTimeMillis()));

        // Set expiry based on plan
        long thirtyDays = 30L * 24 * 60 * 60 * 1000;
        sub.setExpiresAt(new Timestamp(System.currentTimeMillis() + thirtyDays));

        Map<String, Integer> limits = PLAN_LIMITS.getOrDefault(plan, PLAN_LIMITS.get("free"));
        sub.setMaxLiveSessions(limits.get("maxLiveSessions"));
        sub.setMaxSvProjects(limits.get("maxSvProjects"));
        sub.setMaxAiGenerations(limits.get("maxAiGenerations"));
        sub.setMaxStorageMb(limits.get("maxStorageMb"));

        subscriptionRepository.save(sub);
        log.info("[Subscription] 创建/升级订阅: userId={}, plan={}", userId, plan);
        return sub;
    }

    @Override
    public Map<String, Object> checkQuota(Long userId, String metric) {
        Subscription sub = getActiveSubscription(userId);
        String plan = sub != null ? sub.getPlan() : "free";
        Map<String, Integer> limits = PLAN_LIMITS.getOrDefault(plan, PLAN_LIMITS.get("free"));

        int limit = limits.getOrDefault("max" + capitalize(metric), 0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plan", plan);
        result.put("metric", metric);
        result.put("limit", limit);
        result.put("unlimited", limit == -1);
        // Current usage would be computed from UsageRecord - placeholder for now
        result.put("currentUsage", 0);
        result.put("allowed", limit == -1 || 0 < limit);
        return result;
    }

    @Override
    public Map<String, Object> getPlanDetails(String plan) {
        Map<String, Integer> limits = PLAN_LIMITS.getOrDefault(plan, PLAN_LIMITS.get("free"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plan", plan);
        result.put("limits", limits);
        result.put("price", switch (plan) {
            case "pro" -> "\u00A5299/\u6708";
            case "enterprise" -> "\u00A5999/\u6708";
            default -> "\u514D\u8D39";
        });
        result.put("features", switch (plan) {
            case "pro" -> List.of("50\u573A\u76F4\u64AD/\u6708", "100\u4E2A\u77ED\u89C6\u9891\u9879\u76EE", "AI\u65E0\u9650\u751F\u6210", "5GB\u5B58\u50A8");
            case "enterprise" -> List.of("\u4E0D\u9650\u76F4\u64AD\u573A\u6B21", "\u4E0D\u9650\u77ED\u89C6\u9891\u9879\u76EE", "AI\u65E0\u9650\u751F\u6210", "\u4E0D\u9650\u5B58\u50A8", "API\u8BBF\u95EE");
            default -> List.of("5\u573A\u76F4\u64AD/\u6708", "10\u4E2A\u77ED\u89C6\u9891\u9879\u76EE", "50\u6B21AI\u751F\u6210", "500MB\u5B58\u50A8");
        });
        return result;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
