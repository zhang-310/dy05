package cn.gaifan.douyinOperations.module.copy.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class CopyLibraryVO {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String category;
    private String tags;
    private Integer wordCount;
    private Integer useCount;
    private Integer rating;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
