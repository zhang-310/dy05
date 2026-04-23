package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 短视频每日批量生成记录表
 */
@Data
@Entity
@Table(name = "sv_daily_batch")
@SQLRestriction("deleted = 0")
public class SvDailyBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "persona_id")
    private Long personaId;

    /** 来源类型：hot_topic / viral_video / manual */
    @Column(name = "source_type", length = 32, nullable = false)
    private String sourceType;

    @Column(name = "batch_size", nullable = false)
    private int batchSize = 3;

    /** 状态：pending / processing / completed / failed */
    @Column(name = "status", length = 16, nullable = false)
    private String status = "pending";

    /** 结果摘要 JSON */
    @Column(name = "result_summary", columnDefinition = "jsonb")
    private String resultSummary;

    @Column(name = "deleted", nullable = false)
    private int deleted = 0;

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
