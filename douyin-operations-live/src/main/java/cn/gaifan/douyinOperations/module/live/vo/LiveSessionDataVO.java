package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class LiveSessionDataVO {
    private Long id;
    private Long sessionId;
    private Integer totalViewers;
    private Integer peakViewers;
    private Long totalLikes;
    private Integer totalComments;
    private Integer totalShares;
    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private Integer avgStayTime;
    private Integer newFollowers;
    private Timestamp syncTime;
    private String aiAnalysis;
    private Long aiReviewId;
    private Timestamp createTime;
    private Timestamp updateTime;
}
