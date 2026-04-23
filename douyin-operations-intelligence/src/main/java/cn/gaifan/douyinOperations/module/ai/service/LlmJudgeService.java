package cn.gaifan.douyinOperations.module.ai.service;

/**
 * LLM 裁判服务：用 LLM 对进化报告进行结构化评分，替代启发式规则评分
 * 启发式评分作为兜底 fallback（LLM 不可用或超时时降级）
 */
public interface LlmJudgeService {

    /**
     * 对进化报告进行 LLM 裁判评分
     *
     * @param reportType  报告类型：general / huashu / zhishi
     * @param content     报告正文
     * @param topicTitle  进化主题标题（用于上下文）
     * @return 0-100 分；LLM 不可用时返回 -1（调用方降级到启发式）
     */
    int judgeReportQuality(String reportType, String content, String topicTitle);

    /**
     * 批量评分（用于定时任务重算）
     *
     * @param reportType 报告类型
     * @param contents   报告正文列表（同类型）
     * @return 与 contents 等长的分数列表；-1 表示该条评分失败
     */
    java.util.List<Integer> judgeReportQualityBatch(String reportType, java.util.List<String> contents);

    /** 是否可用（模型已配置且连通） */
    boolean isAvailable();
}
