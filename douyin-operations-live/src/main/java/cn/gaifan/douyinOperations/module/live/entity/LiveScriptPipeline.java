package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 直播话术全自动生成流水线
 * 与 sql/live/pipeline-schema.sql 中 live_script_pipeline 一一对应
 */
@Getter
@Setter
@Entity
@Table(name = "live_script_pipeline")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveScriptPipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 流水线状态: pending/generating/checking/refining/completed/failed */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "pending";

    @Column(name = "total_scripts")
    private Integer totalScripts = 0;

    @Column(name = "completed_scripts")
    private Integer completedScripts = 0;

    @Column(name = "refined_scripts")
    private Integer refinedScripts = 0;

    @Column(name = "failed_scripts")
    private Integer failedScripts = 0;

    /** 流水线配置 JSON（modelId, style, useKbRef 等） */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "owner_id", nullable = false)
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
