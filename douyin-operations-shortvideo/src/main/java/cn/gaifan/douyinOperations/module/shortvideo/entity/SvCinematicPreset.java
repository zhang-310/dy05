package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 运镜 Prompt 知识库 (Phase 5)
 * 表: sv_cinematic_preset
 */
@Getter
@Setter
@Entity
@Table(name = "sv_cinematic_preset")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvCinematicPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "camera_type", nullable = false, length = 50)
    private String cameraType;

    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "negative_prompt", columnDefinition = "TEXT")
    private String negativePrompt;

    @Column(name = "quality_level", length = 20)
    private String qualityLevel = "premium-fhd";

    @Column(name = "best_model", length = 50)
    private String bestModel;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate = BigDecimal.ZERO;

    @Column(name = "avg_quality_score", precision = 5, scale = 2)
    private BigDecimal avgQualityScore = BigDecimal.ZERO;

    @Column(name = "use_count")
    private Integer useCount = 0;

    @Column(name = "sample_video_url", length = 500)
    private String sampleVideoUrl;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
