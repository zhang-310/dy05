package cn.gaifan.douyinOperations.module.ai.service.brain;

import cn.gaifan.douyinOperations.module.ai.vo.TrendPredictionVO;

import java.util.List;

/**
 * 实时趋势感知服务（Phase1 + Phase3.4）
 * 数据源：抖音热榜、TianAPI、竞品监控
 * 目标：5分钟内识别新趋势
 */
public interface TrendMonitorService {

    /**
     * 获取当前趋势列表
     *
     * @param category  品类/行业过滤（可选）
     * @param limit     数量
     * @return 趋势信号列表
     */
    List<TrendSignal> getCurrentTrends(String category, int limit);

    /**
     * 获取带生命周期与时间窗口的趋势列表（Phase 3.4）
     */
    List<TrendPredictionVO> getTrendsWithLifecycle(String category, int limit);

    /**
     * 五位主播升级：获取针对某主播个性化的趋势推荐（根据主播人设过滤排序）
     */
    List<TrendSignal> getTrendsForHost(String hostCode, int limit);

    /**
     * 检测并返回新出现的趋势（可用于触发知识进化）
     */
    List<TrendSignal> detectNewTrends();

    /**
     * 定时任务：监控并入库新趋势
     */
    void monitorAndPersist();

    boolean isAvailable();

    record TrendSignal(
            String id,
            String title,
            String category,
            double heatScore,
            long detectedAt,
            String source,
            String description
    ) {}
}
