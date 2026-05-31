package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.List;
import java.util.Map;

/**
 * 用户认知画像服务（Phase1）
 * 维度：内容偏好、表达风格、学习轨迹、心理状态、社交互动模式
 */
public interface UserCognitiveProfileService {

    /**
     * 获取用户认知画像
     */
    UserProfile getProfile(Long userId);

    /**
     * 更新画像（基于行为反馈）
     */
    void updateFromFeedback(Long userId, String actionType, Map<String, Object> feedback);

    /**
     * 获取个性化推荐权重（用于 RAG 排序、话术筛选）
     */
    Map<String, Double> getRecommendationWeights(Long userId);

    /**
     * 五位主播升级：获取针对某主播的用户画像权重（用户偏好 × 主播人设）
     */
    Map<String, Double> getRecommendationWeightsForHost(Long userId, String hostCode);

    boolean isAvailable();

    record UserProfile(
            Long userId,
            Map<String, Double> contentPreferences,
            List<String> expressionStyleTags,
            double learningProgress,
            Map<String, Object> interactionPattern,
            long lastUpdatedAt
    ) {}
}
