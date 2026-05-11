package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 直播数据分析概览 VO
 */
@Data
public class LiveAnalyticsOverviewVO {
    private Long totalSessions;
    private Double totalGmv;
    private Double avgViewers;
}
