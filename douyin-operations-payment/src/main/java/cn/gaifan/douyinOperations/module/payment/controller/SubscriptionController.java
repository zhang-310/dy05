package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.entity.Subscription;
import cn.gaifan.douyinOperations.module.payment.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/current")
    public RESTResult<Subscription> current(@CurrentUserId Long userId) {
        Subscription sub = subscriptionService.getActiveSubscription(userId);
        return RESTResult.success(sub);
    }

    @PostMapping("/upgrade")
    public RESTResult<Subscription> upgrade(@CurrentUserId Long userId, @RequestBody Map<String, String> body) {
        String plan = body.getOrDefault("plan", "free");
        Subscription sub = subscriptionService.createOrUpgrade(userId, plan);
        return RESTResult.success(sub);
    }

    @PostMapping("/check-quota")
    public RESTResult<Map<String, Object>> checkQuota(@CurrentUserId Long userId, @RequestBody Map<String, String> body) {
        String metric = body.getOrDefault("metric", "liveSessions");
        return RESTResult.success(subscriptionService.checkQuota(userId, metric));
    }

    @PostMapping("/plans")
    public RESTResult<Map<String, Object>> plans(@RequestBody(required = false) Map<String, String> body) {
        String plan = body != null ? body.getOrDefault("plan", "free") : "free";
        return RESTResult.success(subscriptionService.getPlanDetails(plan));
    }
}
