package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * Knowledge Library Evolution Log Entity
 * Records all evolution actions: auto-import, version update, archival, deduplication
 */
@Getter
@Setter
@Entity
@Table(name = "ai_knowledge_evolution_log")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class KnowledgeEvolutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "script_version_id", nullable = false)
    private Long scriptVersionId;

    /**
     * Action type: auto_import, version_update, auto_archive, merge, quality_score_update
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /**
     * Rule type: INCLUSION_RULE, UPDATE_RULE, ARCHIVAL_RULE, DEDUP_RULE
     */
    @Column(name = "rule_type", length = 50)
    private String ruleType;

    /**
     * Previous state (JSON format)
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * New state (JSON format)
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Reason for the action
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * Executor: 'system' or user ID
     */
    @Column(name = "executed_by", length = 255)
    private String executedBy = "system";

    /**
     * Status: COMPLETED, FAILED, PENDING
     */
    @Column(name = "status", length = 50)
    private String status = "COMPLETED";

    /**
     * Error message if status = FAILED
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = new Timestamp(System.currentTimeMillis());
        if (updatedAt == null) updatedAt = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
