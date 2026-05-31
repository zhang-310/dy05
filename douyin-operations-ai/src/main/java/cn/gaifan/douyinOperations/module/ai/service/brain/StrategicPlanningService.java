package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.List;
import java.util.Map;

/**
 * 战略规划服务（Phase2）
 * 能力：行业态势、竞品对标、机会点识别、个人SWOT、内容矩阵、增长路径
 */
public interface StrategicPlanningService {

    /**
     * 生成战略规划
     *
     * @param accountId 账号 ID
     * @param userId    用户 ID
     * @param goals     目标（如：万粉突破、5万粉、10万粉）
     * @return 战略规划
     */
    StrategicPlan generate(Long accountId, Long userId, List<String> goals);

    /**
     * 生成战略规划（含上下文：hostCode/strategyPhase/template 可选，用于选择模板）
     */
    default StrategicPlan generate(Long accountId, Long userId, List<String> goals, Map<String, Object> context) {
        return generate(accountId, userId, goals);
    }

    /**
     * 生成增长路径（分阶段目标与策略）
     */
    GrowthPath generateGrowthPath(Long accountId, Long userId, Map<String, Object> currentState);

    boolean isAvailable();

    record StrategicPlan(
            Long accountId,
            String industryAnalysis,
            String competitorAnalysis,
            List<String> opportunityPoints,
            Map<String, Double> swotScores,
            List<ContentMatrixItem> contentMatrix,
            List<PhaseStrategy> growthPhases,
            List<Diagnosis> diagnoses
    ) {
        public StrategicPlan(Long accountId, String industryAnalysis, String competitorAnalysis,
                            List<String> opportunityPoints, Map<String, Double> swotScores,
                            List<ContentMatrixItem> contentMatrix, List<PhaseStrategy> growthPhases) {
            this(accountId, industryAnalysis, competitorAnalysis, opportunityPoints, swotScores, contentMatrix, growthPhases, List.of());
        }
    }

    record Diagnosis(String title, String detail) {}

    record ContentMatrixItem(String type, String strategy, int priority) {}

    record PhaseStrategy(String phase, String goal, List<String> strategies) {}

    record GrowthPath(List<PhaseStrategy> phases, String summary) {}
}
