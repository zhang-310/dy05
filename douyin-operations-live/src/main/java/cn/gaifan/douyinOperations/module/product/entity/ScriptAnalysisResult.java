package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 话术分析结果实体类
 * 存储对特定话术版本的分析结果，包括效果评分、弱点识别、风格识别等
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Entity
@Table(name = "dy_script_analysis_result", indexes = {
    @Index(name = "idx_script_analysis_result_script_version_id", columnList = "script_version_id"),
    @Index(name = "idx_script_analysis_result_owner_id", columnList = "owner_id"),
    @Index(name = "idx_script_analysis_result_overall_score_desc", columnList = "overall_score DESC"),
    @Index(name = "idx_script_analysis_result_created_at_desc", columnList = "created_at DESC"),
    @Index(name = "idx_script_analysis_result_analysis_type", columnList = "analysis_type"),
    @Index(name = "idx_script_analysis_result_deleted", columnList = "deleted")
})
@SQLRestriction("deleted = 0")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptAnalysisResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的话术版本 ID
     */
    @Column(name = "script_version_id", nullable = false)
    private Long scriptVersionId;

    /**
     * 数据隔离：所有者 ID
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 综合评分（0-100）
     */
    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    /**
     * 互动率（%）
     */
    @Column(name = "interaction_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interactionRate;

    /**
     * 转化率（%）
     */
    @Column(name = "conversion_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal conversionRate;

    /**
     * 粉丝增长数
     */
    @Column(name = "fan_growth")
    private Integer fanGrowth;

    /**
     * 评论正面率（0-1）
     */
    @Column(name = "comment_sentiment", precision = 3, scale = 2)
    private BigDecimal commentSentiment;

    /**
     * 主导风格（FRIENDLY/HUMOROUS/PREMIUM/INSPIRATIONAL）
     */
    @Column(name = "dominant_style", length = 64)
    private String dominantStyle;

    /**
     * 弱点识别 JSON 数组
     * JSON 结构：[{"type": "LOW_INTERACTION", "timeRange": "05:30-07:00", "severity": "HIGH", "description": "..."}]
     */
    @Column(name = "weak_points", columnDefinition = "JSONB")
    private String weakPoints;

    /**
     * 分析类型（COMPREHENSIVE/INTERACTION/CONVERSION/COMMENT）
     */
    @Column(name = "analysis_type", nullable = false, length = 64)
    private String analysisType;

    /**
     * 数据来源（LIVE_MONITOR/HISTORICAL）
     */
    @Column(name = "data_source", nullable = false, length = 64)
    private String dataSource;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 逻辑删除标记（0=正常 1=已删除）
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    /**
     * 自动维护创建时间
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.deleted == null) {
            this.deleted = 0;
        }
        if (this.fanGrowth == null) {
            this.fanGrowth = 0;
        }
        if (this.commentSentiment == null) {
            this.commentSentiment = BigDecimal.valueOf(0.5);
        }
    }

    /**
     * 自动维护更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
