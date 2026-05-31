package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 方案素材/版本表，与 sql/shortvideo/schema.sql 中 sv_plan_asset 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "sv_plan_asset")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvPlanAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "asset_type", nullable = false, length = 32)
    private String assetType;  // script / cover / video / image

    @Column(name = "asset_name", length = 256)
    private String assetName;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    @Column(name = "bos_key", length = 500)
    private String bosKey;  // BOS 对象 Key（用于删除）

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_selected", nullable = false)
    private Integer isSelected = 0;

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
