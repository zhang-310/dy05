package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 抖音账号统计 VO
 */
@Data
public class DouyinAccountStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accountId;
    private Long totalVideos;
    private Long totalViews;
    private Long totalLikes;
    private Long totalShares;
    private Long totalComments;
    private Long totalDownloads;
    private Double avgViewsPerVideo;
    private Double avgLikesPerVideo;
}
