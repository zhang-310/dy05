package cn.gaifan.douyinOperations.module.live.service;

public interface LiveOnlineLearningService {
    void updateLearningContext(Long sessionId);
    String getLearningContext(Long sessionId);

    /** 场次结束时提取学习摘要并持久化到跨场次记忆 */
    void extractAndPersistInsights(Long sessionId);
}
