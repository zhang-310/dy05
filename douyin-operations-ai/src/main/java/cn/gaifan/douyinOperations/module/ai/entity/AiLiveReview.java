package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播复盘表，与 sql/ai/evolution-schema.sql 中 ai_live_review 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "ai_live_review")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiLiveReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "total_viewers", nullable = false)
    private Long totalViewers = 0L;

    @Column(name = "total_gmv", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGmv = BigDecimal.ZERO;

    @Column(name = "conversion_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal conversionRate = BigDecimal.ZERO;

    @Column(name = "peak_viewers", nullable = false)
    private Long peakViewers = 0L;

    @Column(name = "top_scripts", columnDefinition = "TEXT")
    private String topScripts;

    @Column(name = "weak_points", columnDefinition = "TEXT")
    private String weakPoints;

    @Column(name = "report_content", columnDefinition = "TEXT")
    private String reportContent;

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
