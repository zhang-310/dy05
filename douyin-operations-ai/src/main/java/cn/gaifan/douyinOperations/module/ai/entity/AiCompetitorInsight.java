package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "ai_competitor_insight")
@SQLRestriction("deleted = 0")
public class AiCompetitorInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(length = 50)
    private String category;

    @Column(name = "competitor_name", length = 100)
    private String competitorName;

    @Column(name = "insight_type", length = 50)
    private String insightType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "collected_at")
    private Timestamp collectedAt;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "ingested_to_kb")
    private Boolean ingestedToKb = false;

    @Column
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createTime = now;
        this.updateTime = now;
        if (this.collectedAt == null) this.collectedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
