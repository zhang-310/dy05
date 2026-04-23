package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prompt 模板实体
 */
@Data
@Entity
@Table(name = "benchmark_prompt_template")
@SQLRestriction("deleted = 0")
public class BenchmarkPromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    private String templateContent;

    @Column(name = "template_variables", columnDefinition = "JSONB")
    private String templateVariables;

    @Column(name = "scene_type", length = 50)
    private String sceneType;

    @Column(name = "industry", length = 50)
    private String industry;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "avg_score", precision = 5, scale = 2)
    private BigDecimal avgScore = BigDecimal.ZERO;

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
