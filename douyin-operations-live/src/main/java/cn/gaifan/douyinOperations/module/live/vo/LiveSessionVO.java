package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 直播场次响应 VO
 */
@Data
public class LiveSessionVO {

    private Long id;
    private Long userId;
    private Long accountId;
    private Long personaId;
    private String liveTitle;
    private String sessionCover;
    private String scriptStyle;
    private String liveDescription;
    private Timestamp scheduledTime;
    private Timestamp scheduledEndTime;
    private Timestamp startTime;
    private Timestamp endTime;
    private String liveUrl;
    private Integer viewers;
    private Long likes;
    private Integer status;
    private String sessionType;
    private String liveFormat;
    private String recordingUrl;
    private Long recordingDuration;
    private Timestamp createTime;
    private Timestamp updateTime;
}
