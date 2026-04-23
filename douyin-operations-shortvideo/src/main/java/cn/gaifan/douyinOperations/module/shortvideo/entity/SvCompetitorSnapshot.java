package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞品数据快照（Phase 4.3）
 */
@Entity
@Table(name = "sv_competitor_snapshot")
public class SvCompetitorSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "competitor_id", nullable = false)
    private Long competitorId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "fan_count")
    private Long fanCount;

    @Column(name = "fan_delta")
    private Integer fanDelta;

    @Column(name = "video_count")
    private Integer videoCount;

    @Column(name = "avg_view_count")
    private Long avgViewCount;

    @Column(name = "avg_like_rate")
    private Double avgLikeRate;

    @Column(name = "avg_completion_rate")
    private Double avgCompletionRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_video_titles", columnDefinition = "jsonb")
    private List<String> topVideoTitles;

    @Column(name = "content_strategy_summary", columnDefinition = "TEXT")
    private String contentStrategySummary;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompetitorId() { return competitorId; }
    public void setCompetitorId(Long competitorId) { this.competitorId = competitorId; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
    public void setSnapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; }
    public Long getFanCount() { return fanCount; }
    public void setFanCount(Long fanCount) { this.fanCount = fanCount; }
    public Integer getFanDelta() { return fanDelta; }
    public void setFanDelta(Integer fanDelta) { this.fanDelta = fanDelta; }
    public Integer getVideoCount() { return videoCount; }
    public void setVideoCount(Integer videoCount) { this.videoCount = videoCount; }
    public Long getAvgViewCount() { return avgViewCount; }
    public void setAvgViewCount(Long avgViewCount) { this.avgViewCount = avgViewCount; }
    public Double getAvgLikeRate() { return avgLikeRate; }
    public void setAvgLikeRate(Double avgLikeRate) { this.avgLikeRate = avgLikeRate; }
    public Double getAvgCompletionRate() { return avgCompletionRate; }
    public void setAvgCompletionRate(Double avgCompletionRate) { this.avgCompletionRate = avgCompletionRate; }
    public List<String> getTopVideoTitles() { return topVideoTitles; }
    public void setTopVideoTitles(List<String> topVideoTitles) { this.topVideoTitles = topVideoTitles; }
    public String getContentStrategySummary() { return contentStrategySummary; }
    public void setContentStrategySummary(String contentStrategySummary) { this.contentStrategySummary = contentStrategySummary; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
