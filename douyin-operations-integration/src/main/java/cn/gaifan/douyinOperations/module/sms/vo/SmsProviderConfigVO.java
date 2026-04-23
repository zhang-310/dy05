package cn.gaifan.douyinOperations.module.sms.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SmsProviderConfigVO {
    private Long id;
    private Long ownerId;
    private String providerCode;
    private String providerName;
    private String signName;
    private String region;
    private Integer status;
    private Integer isDefault;
    private Integer dailyQuota;
    private Integer dailySentCount;
    private Timestamp lastResetTime;
    private Timestamp createTime;
    private Timestamp updateTime;
}
