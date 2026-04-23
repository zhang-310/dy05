package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * A/B 测试日趋势数据 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbDailyTrendVO {
    private LocalDate date;
    private Long variantAViews;
    private Long variantBViews;
    private Long variantAConversions;
    private Long variantBConversions;
    private Double variantAConversionRate;
    private Double variantBConversionRate;
}
