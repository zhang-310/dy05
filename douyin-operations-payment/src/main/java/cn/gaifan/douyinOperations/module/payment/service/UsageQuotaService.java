package cn.gaifan.douyinOperations.module.payment.service;

import java.util.Map;

public interface UsageQuotaService {
    void recordUsage(Long userId, String metric, int delta);
    boolean isWithinQuota(Long userId, String metric);
    int getCurrentMonthUsage(Long userId, String metric);
    Map<String, Object> getUsageQuotaSummary(Long userId);
}
