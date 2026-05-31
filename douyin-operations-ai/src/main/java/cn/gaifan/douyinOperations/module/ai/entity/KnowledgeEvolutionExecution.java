package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * 进化执行记录表
 * 记录每次进化规则执行的统计结果
 */
@Getter
@Setter
@Entity
@Table(name = "ai_knowledge_evolution_execution")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class KnowledgeEvolutionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "execution_period", length = 50)
    private String executionPeriod;

    @Column(name = "execution_date", nullable = false)
    private Date executionDate;

    @Column(name = "rule_type", length = 50)
    private String ruleType;

    @Column(name = "included_count")
    private Integer includedCount = 0;

    @Column(name = "updated_count")
    private Integer updatedCount = 0;

    @Column(name = "merged_count")
    private Integer mergedCount = 0;

    @Column(name = "archived_count")
    private Integer archivedCount = 0;

    @Column(name = "quality_improvement", precision = 5, scale = 2)
    private BigDecimal qualityImprovement;

    @Column(name = "execution_status", length = 50)
    private String executionStatus = "COMPLETED";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_by", length = 255)
    private String executedBy = "system";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
