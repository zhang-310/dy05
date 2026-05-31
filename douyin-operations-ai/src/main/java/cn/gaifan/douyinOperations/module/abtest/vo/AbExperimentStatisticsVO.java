package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * A/B 测试实验统计结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbExperimentStatisticsVO {
    private Long experimentId;
    private String experimentName;
    private Integer status;
    /** 变体统计列表 */
    private List<AbVariantStatsVO> variantStats;
    /** 统计检验结果 */
    private AbStatisticalTestVO statisticalTest;
    /** 日趋势数据 */
    private List<AbDailyTrendVO> dailyTrends;
    /** 总样本量 */
    private Long totalSamples;
    /** 总转化数 */
    private Long totalConversions;
    /** 整体转化率 */
    private Double overallConversionRate;
}
