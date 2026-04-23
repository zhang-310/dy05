package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ViolationWordVO {
    private Long id;
    private String word;
    private Integer level;
    private String reason;
    private String replacement;
    private String scope;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
