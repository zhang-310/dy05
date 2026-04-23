package cn.gaifan.douyinOperations.module.script.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sc_script_variant")
@SQLRestriction("deleted = 0")
public class ScriptVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long generationId;

    @Column(nullable = false)
    private Integer variantIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String keyPoints;

    @Column(nullable = false)
    private Integer likes;

    @Column(nullable = false)
    private Integer uses;

    @Column(nullable = false)
    private LocalDateTime createdTime;

    @Column(nullable = false)
    private Integer deleted;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        deleted = 0;
        if (likes == null) likes = 0;
        if (uses == null) uses = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGenerationId() { return generationId; }
    public void setGenerationId(Long generationId) { this.generationId = generationId; }

    public Integer getVariantIndex() { return variantIndex; }
    public void setVariantIndex(Integer variantIndex) { this.variantIndex = variantIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public String getKeyPoints() { return keyPoints; }
    public void setKeyPoints(String keyPoints) { this.keyPoints = keyPoints; }

    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }

    public Integer getUses() { return uses; }
    public void setUses(Integer uses) { this.uses = uses; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
