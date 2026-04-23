package cn.gaifan.douyinOperations.module.live.service;

import java.util.Map;

public interface ProductStrategyRecommender {
    Map<String, Object> recommend(Long userId, Long productId);
    Map<String, Object> recommendBatchOrder(Long userId, Long sessionId);
}
