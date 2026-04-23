package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.module.payment.entity.Subscription;

import java.util.Map;

public interface SubscriptionService {
    Subscription getActiveSubscription(Long userId);
    Subscription createOrUpgrade(Long userId, String plan);
    Map<String, Object> checkQuota(Long userId, String metric);
    Map<String, Object> getPlanDetails(String plan);
}
