package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SvShotVO {

    private Long id;
    private Long shotListId;
    private Integer shotNumber;
    private String timeRange;
    private String sceneDescription;
    private String cameraAngle;
    private String cameraType;  // 运镜类型 (zoom-in, dolly-in 等)
    private String action;
    private String dialogue;
    private String mood;
    private String keyframeUrl;
    private String keyframeBosKey;
    private String endFrameUrl;
    private String endFrameBosKey;
    private String videoUrl;
    private String videoBosKey;
    private String audioUrl;
    private String audioBosKey;
    private Integer duration;
    private String reviewStatus;   // pending/approved/needs_revision（每日拍摄分镜审核）
    private String reviewerNote;
    private Timestamp createTime;
    private Timestamp updateTime;
}
