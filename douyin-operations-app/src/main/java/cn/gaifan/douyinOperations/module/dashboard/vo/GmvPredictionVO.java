package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * GMV 预测结果：基于历史场均 × 今日商品数 × 时段系数的轻量预测。
 */
@Data
@Builder
public class GmvPredictionVO {
    /** 预测今日 GMV */
    private BigDecimal predictedGmv;
    /** 今日已实现 GMV */
    private BigDecimal actualGmv;
    /** 历史场均 GMV */
    private BigDecimal avgGmvPerSession;
    /** 预测所用历史天数 */
    private int lookbackDays;
    /** 历史场次数 */
    private long historicalSessionCount;
    /** 今日在线/计划场次数 */
    private long todaySessionCount;
    /** 置信级别：high / medium / low（基于历史数据量） */
    private String confidence;
}
