package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SvScriptTemplateVO {
    private Long id;
    private Long ownerId;
    private String templateName;
    private String templateType;
    private String scene;
    private Long categoryId;
    private String content;
    private String description;
    private Integer durationHint;
    private Long useCount;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
