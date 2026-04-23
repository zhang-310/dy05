package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 直播历史趋势分析结果 VO
 */
@Data
public class LiveTrendResultVO {

    /** 账号 ID */
    private Long accountId;

    /** 分析时间范围 */
    private String startDate;
    private String endDate;

    /** 按日期聚合的趋势数据 */
    private List<DailyTrend> dailyTrends;

    /** 按时段聚合的数据 */
    private List<TimeSlotTrend> timeSlotTrends;

    /** 关键指标趋势 */
    private MetricsTrend metricsTrend;

    /** 最佳时段推荐 */
    private BestTimeSlot bestTimeSlot;

    /** 总结分析 */
    private String summary;

    @Data
    public static class DailyTrend {
        private String date;
        private Integer sessionCount; // 直播场次
        private Long totalViewers; // 总观看人数
        private Long avgViewers; // 平均观看人数
        private Long peakViewers; // 峰值观看人数
        private Double avgDuration; // 平均时长（分钟）
        private Long totalGmv; // 总成交额
        private Double conversionRate; // 转化率
    }

    @Data
    public static class TimeSlotTrend {
        private String timeSlot; // morning/afternoon/evening/night
        private String timeSlotName; // 早上/下午/晚上/深夜
        private Integer sessionCount;
        private Long avgViewers;
        private Long peakViewers;
        private Double avgInteractionRate; // 平均互动率
        private Double avgConversionRate; // 平均转化率
    }

    @Data
    public static class MetricsTrend {
        /** 观看人数趋势 */
        private TrendData viewersTrend;

        /** 互动率趋势 */
        private TrendData interactionRateTrend;

        /** 转化率趋势 */
        private TrendData conversionRateTrend;

        /** GMV 趋势 */
        private TrendData gmvTrend;
    }

    @Data
    public static class TrendData {
        private String metricName;
        private List<Double> values;
        private List<String> labels;
        private String trend; // up/down/stable
        private Double changeRate; // 变化率（%）
        private String analysis;
    }

    @Data
    public static class BestTimeSlot {
        private String timeSlot;
        private String timeSlotName;
        private String reason;
        private Map<String, Object> metrics;
    }
}
