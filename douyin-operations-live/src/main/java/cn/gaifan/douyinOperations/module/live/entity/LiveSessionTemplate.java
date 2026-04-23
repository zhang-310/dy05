package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 用户自定义场次槽位结构模板（M-1/M-2）。{@code structure_json} 为槽位序列 JSON 数组。
 */
@Data
@Entity
@Table(name = "live_session_template")
@SQLRestriction("deleted = 0")
public class LiveSessionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 0=系统预置，全租户可读；否则为创建者用户 ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId = 0L;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(length = 512)
    private String description;

    @Column(name = "structure_json", nullable = false, columnDefinition = "TEXT")
    private String structureJson;

    @Column(nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Timestamp(System.currentTimeMillis());
        }
        if (updateTime == null) {
            updateTime = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
