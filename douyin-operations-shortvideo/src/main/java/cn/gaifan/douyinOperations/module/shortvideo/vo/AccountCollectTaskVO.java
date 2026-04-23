package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 账号采集任务返回 VO
 */
@Data
public class AccountCollectTaskVO {

    private Long id;
    private Long ownerId;
    private Long accountId;
    private Long svAccountId;
    private String accountUrl;
    private String accountName;
    private String secUid;
    private String inputType;
    private String originalInput;
    private String status;
    private Integer totalVideos;
    private Integer collectedVideos;
    private Integer analyzedVideos;
    private Integer indexedVideos;
    private Long targetKbId;
    private String errorMessage;
    private Timestamp createTime;
    private Timestamp updateTime;
}
