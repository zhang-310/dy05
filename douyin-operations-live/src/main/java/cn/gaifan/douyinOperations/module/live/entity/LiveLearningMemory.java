package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 跨场次学习记忆：持久化每场直播的学习成果，跨场次沉淀
 */
@Getter
@Setter
@Entity
@Table(name = "live_learning_memory")
@SQLRestriction("deleted = 0")
public class LiveLearningMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_id")
    private Long orgId;

    /** 商品品类（护肤 / 彩妆 / 美容仪器等） */
    @Column(name = "category", length = 50)
    private String category;

    /** 洞察类型：effective_pattern | audience_preference | timing_insight | anti_pattern */
    @Column(name = "insight_type", nullable = false, length = 30)
    private String insightType;

    /** 学习内容 */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 置信度 0.00 ~ 1.00，每次引用但未产生正效果 → confidence *= 0.9；< 0.3 自动归档 */
    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence = BigDecimal.ONE;

    /** 来源场次 ID */
    @Column(name = "source_session_id")
    private Long sourceSessionId;

    /** 使用次数 */
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    /** 最后使用时间 */
    @Column(name = "last_used_at")
    private Timestamp lastUsedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) this.createTime = now;
        if (this.updateTime == null) this.updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
