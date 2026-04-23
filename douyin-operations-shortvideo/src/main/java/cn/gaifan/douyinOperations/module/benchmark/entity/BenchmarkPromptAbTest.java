package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prompt A/B 测试实体
 */
@Data
@Entity
@Table(name = "benchmark_prompt_ab_test")
@SQLRestriction("deleted = 0")
public class BenchmarkPromptAbTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(name = "test_code", nullable = false, length = 50)
    private String testCode;

    @Column(name = "template_a_id", nullable = false)
    private Long templateAId;

    @Column(name = "template_b_id", nullable = false)
    private Long templateBId;

    @Column(name = "traffic_split")
    private Integer trafficSplit = 50;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", length = 20)
    private String status = "draft";

    @Column(name = "winner_template_id")
    private Long winnerTemplateId;

    @Column(name = "total_usage_a")
    private Integer totalUsageA = 0;

    @Column(name = "total_usage_b")
    private Integer totalUsageB = 0;

    @Column(name = "avg_score_a", precision = 5, scale = 2)
    private BigDecimal avgScoreA = BigDecimal.ZERO;

    @Column(name = "avg_score_b", precision = 5, scale = 2)
    private BigDecimal avgScoreB = BigDecimal.ZERO;

    @Column(name = "confidence_level", precision = 5, scale = 2)
    private BigDecimal confidenceLevel;

    @Column(name = "conclusion", columnDefinition = "TEXT")
    private String conclusion;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "deleted")
    private Integer deleted = 0;

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
