package cn.gaifan.douyinOperations.module.ai.service;

/**
 * Prompt 自优化引擎：根据连续低效表现触发 meta-prompt 分析，自动优化 prompt 模板
 */
public interface PromptSelfOptimizationService {

    /**
     * 评估指定任务类型是否需要 prompt 优化（连续 10 次 effectiveness < 60 时触发）
     *
     * @param taskType 任务类型
     * @param userId   用户 ID
     */
    void evaluateAndOptimize(String taskType, Long userId);

    /**
     * 手动触发 prompt 优化
     *
     * @param taskType 任务类型
     * @param userId   用户 ID
     */
    void forceOptimize(String taskType, Long userId);
}
