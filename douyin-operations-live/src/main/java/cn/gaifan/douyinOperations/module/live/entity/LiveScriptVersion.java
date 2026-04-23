package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播话术版本表
 * 与 sql/live/schema.sql 中 live_script_version 一一对应
 * 用于版本管理、对比、推荐等功能
 */
@Data
@Entity
@Table(name = "live_script_version")
@SQLRestriction("deleted = 0")
public class LiveScriptVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原始话术ID（live_script.id） */
    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    /** 直播场次ID（冗余字段，用于快速查询） */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 版本号（从 1 开始） */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** 版本标签（如 v1.0/优化版/最终版） */
    @Column(name = "version_label", length = 64)
    private String versionLabel;

    /** 话术内容 */
    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    /** 话术类型（opening/product/transition/closing/interaction/promotion） */
    @Column(name = "script_type", length = 32)
    private String scriptType;

    /** 创建者备注 */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    /** 版本状态（draft=草稿 / active=活跃 / archived=已归档） */
    @Column(name = "version_status", length = 16)
    private String versionStatus = "draft";

    /** 效果评分（0-100） */
    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore;

    /** 点赞数（作为推荐分数的一部分） */
    @Column(name = "liked_count")
    private Integer likedCount = 0;

    /** 被使用的次数 */
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    /** 最后使用时间 */
    @Column(name = "last_used_time")
    private Timestamp lastUsedTime;

    /** 创建人ID（owner_id，用于数据隔离） */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 是否为推荐版本（0=否 1=是） */
    @Column(name = "is_recommended")
    private Integer isRecommended = 0;

    /** 推荐原因 */
    @Column(name = "recommend_reason", columnDefinition = "TEXT")
    private String recommendReason;

    /** 推荐分数（用于排序推荐版本） */
    @Column(name = "recommend_score", precision = 5, scale = 2)
    private BigDecimal recommendScore;

    /** 基于的前置版本ID（用于版本追溯） */
    @Column(name = "based_on_version_id")
    private Long basedOnVersionId;

    /** 版本变化说明（JSON格式：{changed_fields: [...],...}） */
    @Column(name = "change_summary", columnDefinition = "jsonb")
    private String changeSummary;

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
