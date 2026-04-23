package cn.gaifan.douyinOperations.module.sms.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SmsTemplateVO {
    private Long id;
    private Long ownerId;
    private String templateCode;
    private String templateName;
    private String content;
    private String providerCode;
    private String providerTemplateId;
    private Integer status;
    private String templateType;
    private String remark;
    private Timestamp createTime;
    private Timestamp updateTime;
}
