package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.List;
import java.util.Map;

/**
 * 账号诊断系统（Phase2）
 * 多维评估：定位清晰度、内容竞争力、增长健康度、风险等级
 */
public interface AccountDiagnosisService {

    /**
     * 账号全方位诊断
     *
     * @param accountId 抖音账号 ID
     * @param userId    用户 ID
     * @return 诊断结果
     */
    DiagnosisResult diagnose(Long accountId, Long userId);

    /**
     * 批量诊断（用于机构多账号管理）
     */
    List<DiagnosisResult> diagnoseBatch(List<Long> accountIds, Long userId);

    /**
     * 内容诊断：分析话术效果与行业对比
     */
    Map<String, Object> diagnoseContent(Long userId, String category);

    /**
     * 选品诊断：分析商品讲解策略
     */
    Map<String, Object> diagnoseProductStrategy(Long userId);

    /**
     * 节奏诊断：分析直播时段观众流失
     */
    Map<String, Object> diagnoseRhythm(Long userId);

    boolean isAvailable();

    record DiagnosisResult(
            Long accountId,
            double positioningClarity,   // 定位清晰度 0-1
            double contentCompetitiveness,
            double growthHealth,
            String riskLevel,            // 低/中/高
            List<String> suggestedPriorities,
            String summary,
            boolean isEstimated         // 无数据时为 true，指标为估算值
    ) {
        public DiagnosisResult(Long accountId, double positioningClarity, double contentCompetitiveness,
                double growthHealth, String riskLevel, List<String> suggestedPriorities, String summary) {
            this(accountId, positioningClarity, contentCompetitiveness, growthHealth, riskLevel, suggestedPriorities, summary, false);
        }
    }
}
