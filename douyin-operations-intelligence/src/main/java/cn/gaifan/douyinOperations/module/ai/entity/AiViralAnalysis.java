package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 爆款拆解分析表，与 sql/ai/evolution-schema.sql 中 ai_viral_analysis 一一对应
 */
@Data
@Entity
@Table(name = "ai_viral_analysis")
@SQLRestriction("deleted = 0")
public class AiViralAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "viral_score", nullable = false)
    private Integer viralScore = 0;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "avg_view_count", nullable = false)
    private Long avgViewCount = 0L;

    @Column(name = "success_factors", columnDefinition = "TEXT")
    private String successFactors;

    @Column(name = "replicable_methods", columnDefinition = "TEXT")
    private String replicableMethods;

    @Column(name = "report_content", columnDefinition = "TEXT")
    private String reportContent;

    @Column(name = "quality_score", nullable = false)
    private Integer qualityScore = 0;

    @Column(name = "model_used", length = 64)
    private String modelUsed;

    @Column(name = "tokens_used", nullable = false)
    private Long tokensUsed = 0L;

    /** 状态：0=分析中 1=完成 2=失败 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

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
