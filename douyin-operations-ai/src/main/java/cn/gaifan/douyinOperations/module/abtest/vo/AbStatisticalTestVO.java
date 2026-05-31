package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A/B 测试统计检验结果 VO（卡方检验）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbStatisticalTestVO {
    /** 卡方统计量 */
    private Double chiSquare;
    /** p-value（显著性水平） */
    private Double pValue;
    /** 置信度（1 - pValue） */
    private Double confidenceLevel;
    /** 是否有显著差异（p < 0.05） */
    private Boolean isSignificant;
    /** 推荐的赢家变体 ID */
    private Long recommendedWinnerId;
    /** 推荐的赢家变体名称 */
    private String recommendedWinnerName;
    /** 统计分析说明 */
    private String conclusion;
}
