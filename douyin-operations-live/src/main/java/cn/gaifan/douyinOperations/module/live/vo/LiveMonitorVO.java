package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 直播监控数据响应 VO
 */
@Data
public class LiveMonitorVO {

    private Long id;
    private Long sessionId;
    private Timestamp timestamp;
    private Integer viewers;
    private Long likes;
    private Integer comments;
    private Integer shares;
    private Integer productImpressions;
    private Timestamp createTime;
}
