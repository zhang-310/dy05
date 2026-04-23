package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * P1 D5-03：跨模块统一 KPI（内容 / 流量 / 转化 / 营收），与 {@link BusinessDashboardVO} 互补聚合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedKpiVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 内容：短视频、商品话术、知识库文档 */
    private ContentBlock content;
    /** 流量：直播观众累计、短视频播放 */
    private TrafficBlock traffic;
    /** 转化：销量件数、带货条数 */
    private ConversionBlock conversion;
    /** 营收：GMV、客单价 */
    private RevenueBlock revenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentBlock implements Serializable {
        private static final long serialVersionUID = 1L;
        private long shortVideoCount;
        private long productScriptCount;
        private long kbDocumentCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrafficBlock implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 历史场次观众数汇总（live_session.viewers） */
        private long liveViewerSum;
        /** 短视频累计播放量（owner 范围内） */
        private long shortVideoViewSum;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversionBlock implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 今日起排品销量件数汇总 */
        private long saleQuantitySinceToday;
        /** 今日起产生营收的排品条数 */
        private long revenueLinesSinceToday;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueBlock implements Serializable {
        private static final long serialVersionUID = 1L;
        private BigDecimal todayGmv;
        private BigDecimal yesterdayGmv;
        /** 今日 GMV / max(今日销量件数,1) */
        private BigDecimal avgOrderValueToday;
    }
}
