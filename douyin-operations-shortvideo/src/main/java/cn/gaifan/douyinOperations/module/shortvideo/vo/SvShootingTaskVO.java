package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SvShootingTaskVO {

    private Long id;
    private Long ownerId;
    private Long anchorUserId;
    private Long personaId;
    private Long photographerId;
    private Long scriptId;
    private Long projectId;
    private String title;
    private String description;
    private String scriptContent;
    private String shootingBrief;
    private String shootDate;
    private Integer priority;
    private Integer status;
    private String materialUrls;
    private String reviewNotes;
    private Long reviewedBy;
    private String referenceVideoUrl;
    private Long referenceVideoTaskId;
    private Timestamp referenceGeneratedAt;
    private Timestamp createTime;
    private Timestamp updateTime;
}
