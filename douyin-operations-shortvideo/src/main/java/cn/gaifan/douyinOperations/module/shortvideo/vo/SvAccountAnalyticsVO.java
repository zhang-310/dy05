package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * 账号下已采集视频的综合统计（基于 sv_viral_video 聚合，随账号维度变化）
 */
@Data
public class SvAccountAnalyticsVO {

    /** 已采集视频条数 */
    private int videoCount;

    private long sumViewCount;
    private long sumLikeCount;
    private long sumShareCount;

    /** 条均（视频维度平均，便于对比单条表现） */
    private double avgViewPerVideo;
    private double avgLikePerVideo;
    private double avgSharePerVideo;

    /** 爆款评分均值（库内 viral_score） */
    private double avgViralScore;

    private long maxViewCount;
    private long minViewCount;

    private int deepPendingCount;
    private int deepProcessingCount;
    private int deepCompletedCount;
    private int deepFailedCount;
    /** 状态为空或其它 */
    private int deepOtherCount;
}
