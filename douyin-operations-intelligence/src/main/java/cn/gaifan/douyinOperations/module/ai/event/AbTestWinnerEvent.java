package cn.gaifan.douyinOperations.module.ai.event;

/**
 * A/B 测试胜出事件：当 A/B 实验达到统计显著性（p < 0.05）时发布
 *
 * @param experimentKey  实验标识
 * @param winnerVariant  胜出变体
 * @param metric         胜出指标（conversionRate / effectivenessScore）
 * @param improvementPct 改善百分比
 */
public record AbTestWinnerEvent(
        String experimentKey,
        String winnerVariant,
        String metric,
        double improvementPct
) {
}
