package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class SvCategoryVO {
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private Integer sortOrder;
    private Timestamp createTime;
    private Timestamp updateTime;
}
