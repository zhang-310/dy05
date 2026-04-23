package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 话术质量评分（合规/流畅/吸引力三维评分）
 */
@Data
@Entity
@Table(name = "live_script_quality_score")
@SQLRestriction("deleted = 0")
public class LiveScriptQualityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "session_id")
    private Long sessionId;

    /** 合规评分 0-100 */
    @Column(name = "compliance_score", precision = 5, scale = 2)
    private BigDecimal complianceScore = BigDecimal.ZERO;

    /** 流畅度评分 0-100 */
    @Column(name = "fluency_score", precision = 5, scale = 2)
    private BigDecimal fluencyScore = BigDecimal.ZERO;

    /** 吸引力评分 0-100 */
    @Column(name = "engagement_score", precision = 5, scale = 2)
    private BigDecimal engagementScore = BigDecimal.ZERO;

    /** 综合质量评分 0-100 */
    @Column(name = "total_quality_score", precision = 5, scale = 2)
    private BigDecimal totalQualityScore = BigDecimal.ZERO;

    /** 评分明细 JSON */
    @Column(name = "details_json", columnDefinition = "JSONB")
    private String detailsJson;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
