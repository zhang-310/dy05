package cn.gaifan.douyinOperations.module.sms.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class SmsSendLogVO {
    private Long id;
    private Long ownerId;
    private String phoneNumber;
    private String templateCode;
    private String providerCode;
    private String providerRequestId;
    private String content;
    private String status;
    private String errorMessage;
    private String errorCode;
    private Timestamp sendTime;
    private Timestamp deliveredTime;
    private BigDecimal cost;
    private String bizId;
    private String bizType;
    private Timestamp createTime;
    private Timestamp updateTime;
}
