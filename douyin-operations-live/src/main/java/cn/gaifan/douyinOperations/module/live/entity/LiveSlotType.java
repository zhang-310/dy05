package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 直播槽位类型配置表
 */
@Getter
@Setter
@Entity
@Table(name = "live_slot_type")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveSlotType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_code", nullable = false, unique = true, length = 32)
    private String slotCode;

    @Column(name = "slot_label", nullable = false, length = 64)
    private String slotLabel;

    @Column(name = "default_requirement", length = 128)
    private String defaultRequirement;

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
