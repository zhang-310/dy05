package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 账号列表返回 VO
 */
@Data
public class SvAccountVO {

    private Long id;
    private String secUid;
    private String douyinId;
    private String nickname;
    private String avatarUrl;
    private String signature;

    // 账号数据
    private Long followerCount;
    private Long followingCount;
    private Long totalFavorited;
    private Integer videoCount;

    // 认证信息
    private Boolean isVerified;
    private String verificationType;

    // 采集统计
    private Integer collectCount;
    private Timestamp lastCollectTime;
    private Integer totalCollectedVideos;

    // 分析统计
    private Long avgViewCount;
    private Integer avgLikeCount;
    private BigDecimal avgViralScore;
    private BigDecimal topViralScore;

    // 标签与分类
    private String industryTags;
    private String contentTags;
    private String accountCategory;

    // 来源信息
    private String sourceType;
    private String sourceKeyword;

    // 状态
    private String status;
    private String notes;

    private Timestamp createTime;
    private Timestamp updateTime;
}
