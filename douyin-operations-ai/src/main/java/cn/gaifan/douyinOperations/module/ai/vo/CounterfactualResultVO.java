package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

import java.util.List;

/**
 * 反事实推理结果（Phase 3.2）
 */
@Data
public class CounterfactualResultVO {
    /** 干预前预测转化率 */
    private Double expectedConversionRateBefore;
    /** 干预后预测转化率 */
    private Double expectedConversionRateAfter;
    /** 预测的转化率变化（如 +0.023 表示 +2.3%） */
    private Double conversionRateDelta;
    /** 影响路径，如 scriptType → retentionRate → conversionRate */
    private List<String> impactPath;
    /** 置信区间下限 */
    private Double confidenceLower;
    /** 置信区间上限 */
    private Double confidenceUpper;
    /** 建议文案 */
    private String suggestion;
}
