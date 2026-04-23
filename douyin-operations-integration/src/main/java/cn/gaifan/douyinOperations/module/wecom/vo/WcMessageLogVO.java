package cn.gaifan.douyinOperations.module.wecom.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class WcMessageLogVO {
    private Long id;
    private Long ownerId;
    private Long robotId;
    private Long ruleId;
    private String messageType;
    private String messageContent;
    private Integer status;
    private String errorMessage;
    private Timestamp sendTime;
    private Timestamp createTime;
}
