package cn.gaifan.douyinOperations.module.copy.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class CopyTemplateVO {
    private Long id;
    private Long userId;
    private String templateName;
    private String templateContent;
    private String category;
    private String description;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
