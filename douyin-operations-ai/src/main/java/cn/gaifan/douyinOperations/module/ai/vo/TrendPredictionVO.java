package cn.gaifan.douyinOperations.module.ai.vo;

import cn.gaifan.douyinOperations.module.ai.service.brain.TrendMonitorService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 趋势预测结果（Phase 3.4）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPredictionVO {
    private TrendMonitorService.TrendSignal signal;
    private TrendLifecycle lifecycle;
    private HotspotWindow window;
}
