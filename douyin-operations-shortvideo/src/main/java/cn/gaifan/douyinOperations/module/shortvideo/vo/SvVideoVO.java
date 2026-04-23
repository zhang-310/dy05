package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SvVideoVO {
    private Long id;
    private Long ownerId;
    private Long accountId;
    private String douyinVideoId;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer duration;
    private String tags;
    private Long categoryId;
    private Timestamp publishTime;
    private Long viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer favoriteCount;
    private Boolean isViral;
    private Long aiCallLogId;
    private Boolean aiGenerated;
    private Long planId;
    private String syncStatus;
    private Timestamp createTime;
    private Timestamp updateTime;
}
