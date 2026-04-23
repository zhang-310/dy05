package cn.gaifan.douyinOperations.module.douyin.service;

/**
 * 抖音话术学习管线：从热门视频/评论中提取话术模式并入库。
 * P1-1: 实装抖音热门话术学习管线。
 */
public interface DouyinScriptLearningService {

    /**
     * 执行完整的话术学习管线：
     * 1) 获取热门抖音视频（优先通过 API，回退到鬼鬼鸭热榜）
     * 2) 获取视频评论，提取高频关注点
     * 3) 用 LLM 从视频描述+评论中提取话术模式
     * 4) 写入 script_library（source=douyin_learn）
     */
    void runLearningPipeline();

    /**
     * 针对指定账号执行话术学习（需要 accessToken）
     */
    void runLearningForAccount(Long accountId, String accessToken);
}
