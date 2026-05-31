package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势生命周期（Phase 3.4）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendLifecycle {
    /** emerging / rising / peak / declining / dead */
    private String phase;
    /** 动量（热度变化率） */
    private double momentum;
    /** 预计达到峰值的小时数 */
    private int estimatedPeakHours;
    private double currentHeat;
    private double predictedPeakHeat;
}
