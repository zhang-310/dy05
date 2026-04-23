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
    private String liveTitle;
    private String liveDescription;
    private Timestamp scheduledTime;
    private Timestamp startTime;
    private Timestamp endTime;
    private String liveUrl;
    private Integer viewers;
    private Long likes;
    private Integer status;
    private String recordingUrl;
    private Long recordingDuration;
    private Timestamp createTime;
    private Timestamp updateTime;
}
