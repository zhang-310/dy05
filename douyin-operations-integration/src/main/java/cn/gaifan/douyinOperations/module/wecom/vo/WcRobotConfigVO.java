package cn.gaifan.douyinOperations.module.wecom.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class WcRobotConfigVO {
    private Long id;
    private Long ownerId;
    private String robotName;
    private String webhookUrl;
    private String robotType;
    private Integer status;
    private String description;
    private Timestamp createTime;
    private Timestamp updateTime;
}
