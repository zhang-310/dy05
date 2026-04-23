package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ScriptVO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String category;
    private String source;
    private Long sourceId;
    private String tags;
    private Long useCount;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
