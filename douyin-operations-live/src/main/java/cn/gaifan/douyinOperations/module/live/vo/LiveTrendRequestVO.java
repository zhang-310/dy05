package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 直播历史趋势分析请求 VO
 */
@Data
public class LiveTrendRequestVO {

    /** 账号 ID */
    private Long accountId;

    /** 开始日期（格式：yyyy-MM-dd） */
    private String startDate;

    /** 结束日期（格式：yyyy-MM-dd） */
    private String endDate;

    /** 聚合维度：daily（按日）、time_slot（按时段） */
    private String aggregateBy;
}
