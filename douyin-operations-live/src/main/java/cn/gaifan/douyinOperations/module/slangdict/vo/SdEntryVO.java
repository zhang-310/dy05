package cn.gaifan.douyinOperations.module.slangdict.vo;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

@Data
public class SdEntryVO {
    private Long id;
    private Long userId;
    private String phrase;
    private String meaning;
    private String category;
    private String usageScene;
    private String example;
    private String source;
    private Integer useCount;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
    /** 关联的产品 ID 列表 */
    private List<Long> productIds;
}
