package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对标账号返回VO
 */
@Data
public class BenchmarkAccountVO {

    private Long id;
    private String accountName;
    private String platform;
    private String accountUrl;
    private String secUid;
    private String douyinId;
    private String category;
    private Long fanCount;
    private Integer videoCount;
    private Long avgViewCount;
    private Integer avgLikeCount;
    private String notes;
    private Boolean isActive;
    private LocalDateTime lastCollectTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
