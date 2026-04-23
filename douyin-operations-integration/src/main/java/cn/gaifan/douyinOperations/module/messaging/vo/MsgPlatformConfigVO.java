package cn.gaifan.douyinOperations.module.messaging.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class MsgPlatformConfigVO {

    private Long id;
    private Long ownerId;
    private String platform;
    private String appId;
    private String corpId;
    private Long agentId;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
