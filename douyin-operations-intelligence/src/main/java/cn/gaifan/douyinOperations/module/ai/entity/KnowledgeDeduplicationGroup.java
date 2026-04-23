package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Knowledge Deduplication Group Entity
 * Groups duplicate/variant scripts together with similarity scores
 */
@Data
@Entity
@Table(name = "ai_knowledge_deduplication_group")
@SQLRestriction("deleted = 0")
public class KnowledgeDeduplicationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Master (primary) script version ID
     */
    @Column(name = "master_script_id", nullable = false)
    private Long masterScriptId;

    /**
     * Duplicate (variant) script version ID
     */
    @Column(name = "duplicate_script_id", nullable = false)
    private Long duplicateScriptId;

    /**
     * Vector similarity score 0-1
     */
    @Column(name = "similarity_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal similarityScore;

    /**
     * Merge status: DETECTED, MERGED, IGNORED, PENDING_REVIEW
     */
    @Column(name = "merge_status", length = 50)
    private String mergeStatus = "DETECTED";

    /**
     * Reason for merge
     */
    @Column(name = "merge_reason", length = 255)
    private String mergeReason;

    /**
     * When the merge was completed
     */
    @Column(name = "merged_at")
    private Timestamp mergedAt;

    /**
     * Variant type: VARIANT, ARCHIVED, PENDING
     */
    @Column(name = "variant_type", length = 50)
    private String variantType = "VARIANT";

    /**
     * 1 = active, 0 = archived
     */
    @Column(name = "is_active")
    private Integer isActive = 1;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
