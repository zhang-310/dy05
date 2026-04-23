package cn.gaifan.douyinOperations.module.live.service;


/**
 * 跨场次学习服务：场次结束时提取学习摘要，下一场开播时加载历史记忆
 */
public interface CrossSessionLearningService {

    /**
     * 场次结束时提取并持久化学习洞察
     *
     * @param sessionId 场次 ID
     */
    void extractAndPersistInsights(Long sessionId);

    /**
     * 加载同品类最近 5 场的学习记忆，注入初始 prompt
     *
     * @param userId   用户 ID
     * @param category 商品品类
     * @return 格式化的学习记忆上下文
     */
    String loadCrossSessionContext(Long userId, String category);

    /**
     * 衰减未产生正效果的记忆置信度
     */
    void decayIneffectiveMemories();

    /**
     * P1-3: 根据话术效果强化或弱化记忆置信度
     *
     * @param userId        用户 ID
     * @param liveFormat    直播形态
     * @param insightType   洞察类型
     * @param positive      效果好（置信度 x1.1）还是差（x0.8）
     */
    void reinforceMemory(Long userId, String liveFormat, String insightType, boolean positive);
}
