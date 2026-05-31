package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 创作方案表，与 sql/shortvideo/schema.sql 中 sv_plan 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "sv_plan")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "target_platform", length = 32)
    private String targetPlatform;

    @Column(name = "plan_status", nullable = false)
    private Integer planStatus = 0;  // 0=草稿 1=执行中 2=已完成

    @Column(name = "ai_generated", nullable = false)
    private Integer aiGenerated = 0;

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
