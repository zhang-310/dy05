package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * Evolution Rule Configuration Entity
 * Stores configuration for the 4 core evolution rules
 */
@Data
@Entity
@Table(name = "ai_knowledge_evolution_rule")
@SQLRestriction("deleted = 0")
public class EvolutionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Rule type: INCLUSION_RULE, UPDATE_RULE, ARCHIVAL_RULE, DEDUP_RULE
     */
    @Column(name = "rule_type", nullable = false, unique = true, length = 50)
    private String ruleType;

    /**
     * Human-readable rule name
     */
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    /**
     * Rule description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Is rule enabled
     */
    @Column(name = "is_enabled")
    private Integer isEnabled = 1;

    /**
     * Rule configuration parameters (JSON)
     * Examples:
     * INCLUSION_RULE: {"score_threshold": 80, "min_usage_count": 5, "max_daily_import": 100}
     * UPDATE_RULE: {"score_improvement_threshold": 5, "min_new_version_score": 75, "stability_days": 14}
     * ARCHIVAL_RULE: {"consecutive_low_score_periods": 3, "low_score_threshold": 40}
     * DEDUP_RULE: {"similarity_threshold": 0.85, "require_manual_approval": false}
     */
    @Column(name = "config", columnDefinition = "JSONB")
    private String config;

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
