package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 抖音视频 VO（列表/详情）
 */
@Data
public class DouyinVideoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long accountId;
    private String videoId;
    private String title;
    private String description;
    private Long viewCount;
    private Long likeCount;
    private Long shareCount;
    private Long commentCount;
    private Long downloadCount;
    private String videoType;
    private Timestamp publishTime;
    private Timestamp createTime;
    private Timestamp updateTime;
}
