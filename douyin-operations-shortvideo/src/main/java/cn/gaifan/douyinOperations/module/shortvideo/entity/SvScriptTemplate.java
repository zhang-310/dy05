package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 短视频脚本模板表
 * 与 sql/shortvideo/migration-script-template.sql 中 sv_script_template 一一对应
 */
@Data
@Entity
@Table(name = "sv_script_template")
@SQLRestriction("deleted = 0")
public class SvScriptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "template_name", nullable = false, length = 256)
    private String templateName;

    /** 模板类型：system=系统预设 user=用户自建 */
    @Column(name = "template_type", nullable = false, length = 32)
    private String templateType = "system";

    /** 适用场景：hook/body/cta/full */
    @Column(name = "scene", length = 64)
    private String scene;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "description", length = 512)
    private String description;

    /** 建议时长(秒) */
    @Column(name = "duration_hint")
    private Integer durationHint;

    @Column(name = "use_count", nullable = false)
    private Long useCount = 0L;

    /** 状态：0=禁用 1=有效 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

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
