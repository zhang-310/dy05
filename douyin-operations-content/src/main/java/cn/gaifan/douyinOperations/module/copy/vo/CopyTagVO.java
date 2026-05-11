package cn.gaifan.douyinOperations.module.copy.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文案标签 VO
 */
@Data
public class CopyTagVO {
    private Long id;
    private String name;
    private String category;
    private Integer usageCount;
    private LocalDateTime createTime;
}
