package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分析任务实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_task")
@NoArgsConstructor
public class BenchmarkTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "benchmark_account_id")
    private Long benchmarkAccountId;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "task_status", nullable = false, length = 16)
    private String taskStatus = "pending";

    @Column(name = "progress")
    private Integer progress = 0;

    @Column(name = "total_videos")
    private Integer totalVideos = 0;

    @Column(name = "processed_videos")
    private Integer processedVideos = 0;

    @Column(name = "failed_videos")
    private Integer failedVideos = 0;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
