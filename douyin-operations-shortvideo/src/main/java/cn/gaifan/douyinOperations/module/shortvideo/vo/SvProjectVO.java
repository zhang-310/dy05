package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * 短视频项目 VO
 */
@Data
public class SvProjectVO {

    private Long id;
    private Long ownerId;
    private Long accountId;
    private String title;
    private String projectType;
    private Long personaId;
    private Date scheduleDate;
    private String shootStatus;
    private String status;
    private Long scriptId;
    private Long shotListId;
    private String finalVideoUrl;
    private String thumbnailUrl;
    private String characterReferenceUrl;
    private String sceneReferenceUrl;
    private Integer duration;
    private List<Long> relatedProductIds;
    private String publishTitle;
    private String publishPlatforms;
    private Timestamp publishTime;
    private String reviewStatus;
    private Long reviewerId;
    private Timestamp reviewTime;
    private String reviewComment;
    private Timestamp createTime;
    private Timestamp updateTime;
}
