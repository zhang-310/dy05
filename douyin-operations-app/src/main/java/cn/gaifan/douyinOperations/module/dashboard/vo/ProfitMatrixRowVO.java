package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 利润矩阵预览单行：GMV 来自已结束场次汇总；毛利率优先使用真实成本，无成本时用占位。
 */
@Data
public class ProfitMatrixRowVO {

    private String liveFormat;
    private long sessionCount;
    private BigDecimal totalGmv;
    /** 0–1 之间的毛利率（真实或占位） */
    private BigDecimal estimatedMarginRate;
    /** true=占位（成本数据缺失），false=基于真实 cost_price */
    private boolean isEstimated;
    private String note;
}
