package cn.gaifan.douyinOperations.module.product.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 风格预设模板表
 */
@Data
@Entity
@Table(name = "style_preset")
@SQLRestriction("deleted = 0")
public class StylePreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preset_name", nullable = false, length = 64)
    private String presetName;

    @Column(name = "preset_code", nullable = false, unique = true, length = 64)
    private String presetCode;

    @Column(name = "style_value", nullable = false, length = 128)
    private String styleValue;

    @Column(name = "style_tags_json", columnDefinition = "jsonb")
    private String styleTagsJson;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "word_count_min")
    private Integer wordCountMin = 150;

    @Column(name = "word_count_max")
    private Integer wordCountMax = 300;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "created_by")
    private Long createdBy;

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
