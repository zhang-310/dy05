package cn.gaifan.douyinOperations.module.wecom.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class WcPushRuleVO {
    private Long id;
    private Long ownerId;
    private Long robotId;
    private String ruleName;
    private String triggerType;
    private String triggerConfig;
    private String messageTemplate;
    private Integer status;
    private Timestamp lastTriggerTime;
    private Timestamp createTime;
    private Timestamp updateTime;
}
