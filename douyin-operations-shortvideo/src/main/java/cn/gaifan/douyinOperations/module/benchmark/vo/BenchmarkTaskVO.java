package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务返回VO
 */
@Data
public class BenchmarkTaskVO {

    private Long id;
    private Long benchmarkAccountId;
    private String taskType;
    private String taskStatus;
    private Integer progress;
    private Integer totalVideos;
    private Integer processedVideos;
    private Integer failedVideos;
    private String configJson;
    private String resultSummary;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
