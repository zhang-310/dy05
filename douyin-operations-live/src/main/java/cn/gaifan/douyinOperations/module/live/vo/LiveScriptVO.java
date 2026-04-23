package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 直播话术响应 VO
 */
@Data
public class LiveScriptVO {

    private Long id;
    private Long sessionId;
    private String scriptContent;
    private Integer sequenceNo;
    private Long executionTime;
    private Integer executed;
    private Timestamp actualExecutionTime;
    private Timestamp createTime;
    private Timestamp updateTime;
}
