package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 生成配置预设表
 * 保存用户的话术生成偏好（风格、模型、知识库引用、时长模式、热门关键词等）
 * 与 sql/live/generation-preset-schema.sql 中 live_generation_preset 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "live_generation_preset")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveGenerationPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "style", length = 50)
    private String style = "standard";

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "use_kb_ref")
    private Boolean useKbRef = true;

    /** standard / short / custom */
    @Column(name = "duration_mode", length = 20)
    private String durationMode = "standard";

    /** 热门关键词（JSON 数组或逗号分隔） */
    @Column(name = "hot_keywords", columnDefinition = "TEXT")
    private String hotKeywords;

    /** IP 类型：phenomenal / top / 空 */
    @Column(name = "ip_type", length = 30)
    private String ipType;

    /** 素材类型：joke / chicken_soup / quote / interactive_game / 空 */
    @Column(name = "material_type", length = 30)
    private String materialType;

    /** 话术模块：emotion_drive / value_creation / conversion_engine / trust_reinforcement / 空 */
    @Column(name = "script_module", length = 40)
    private String scriptModule;

    /** 留人策略：high_suspense / high_practical / high_climax / 空 */
    @Column(name = "retention_strategy", length = 30)
    private String retentionStrategy;

    /** 互动等级：light / medium / heavy / 空 */
    @Column(name = "interaction_level", length = 20)
    private String interactionLevel;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    /** 所属用户 ID（数据隔离） */
    @Column(name = "owner_id")
    private Long ownerId;

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
