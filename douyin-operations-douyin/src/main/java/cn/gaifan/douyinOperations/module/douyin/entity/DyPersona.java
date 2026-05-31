package cn.gaifan.douyinOperations.module.douyin.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 抖音人设表，与 sql/douyin/schema.sql 中 dy_persona 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "dy_persona")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class DyPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "persona_name", nullable = false, length = 128)
    private String personaName;

    @Column(name = "persona_type", length = 32)
    private String personaType;  // knowledge / entertainment / lifestyle / commerce

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tone", length = 64)
    private String tone;  // 语气风格：professional / casual / humorous / warm

    @Column(name = "target_audience", length = 256)
    private String targetAudience;

    @Column(name = "content_style", columnDefinition = "TEXT")
    private String contentStyle;

    @Column(name = "keywords", length = 512)
    private String keywords;

    @Column(name = "is_default", nullable = false)
    private Integer isDefault = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 互动风格（积极/平稳/活跃等） */
    @Column(name = "interaction_style", length = 64)
    private String interactionStyle;

    /** 语言风格（简洁/专业/亲切等） */
    @Column(name = "language_style", length = 64)
    private String languageStyle;

    /** 内容比例配置（JSON，如互动:产品介绍:催单 = 3:5:2） */
    @Column(name = "content_ratio", length = 256)
    private String contentRatio;

    /** 本地特色/方言风格 */
    @Column(name = "local_flavor", length = 64)
    private String localFlavor;

    /** 直播风格（专业知识/生活分享/娱乐互动等） */
    @Column(name = "live_style", length = 64)
    private String liveStyle;

    /** 人设特质标签（JSON 数组） */
    @Column(name = "persona_traits", length = 512)
    private String personaTraits;

    /** IP 类型（knowledge/entertainment/lifestyle 等） */
    @Column(name = "ip_type", length = 32)
    private String ipType;

    /** 目标年龄段（如 18-24/25-35） */
    @Column(name = "age_range", length = 32)
    private String ageRange;

    /** 定位标签（JSON 数组） */
    @Column(name = "positioning_tags", length = 512)
    private String positioningTags;

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
