package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * AI 运营看板服务：调用量趋势、额度趋势
 */
public interface AiDashboardService {

    /**
     * 调用量趋势（按日，最近 N 天）
     * @param callType 可选，按调用类型筛选
     */
    List<Map<String, Object>> getCallVolumeTrend(int days, String callType);

    /**
     * 调用量趋势（按小时，最近 N 小时）
     * @param callType 可选，按调用类型筛选
     */
    List<Map<String, Object>> getCallVolumeTrendByHour(int hours, String callType);

    /**
     * 额度使用趋势（按日，最近 N 天）
     */
    List<Map<String, Object>> getQuotaTrend(int days);

    /**
     * 按 call_type 分布（最近 N 天）
     */
    List<Map<String, Object>> getCallTypeDistribution(int days);

    /**
     * 仪表盘概览统计（今日调用、本月 Token、成功率、质量均分）
     */
    Map<String, Object> getDashboardStats();

    /**
     * 成本/用量拆解：按 call_type 汇总 tokens与调用次数（最近 N 天）
     */
    List<Map<String, Object>> getCostBreakdown(int days);
}
