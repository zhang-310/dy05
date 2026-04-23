package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ScriptTemplateVO {
    private Long id;
    private String templateName;
    private String templateType;
    private String scene;
    private String content;
    private String description;
    private Long userId;
    private Long useCount;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
