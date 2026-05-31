package cn.gaifan.douyinOperations.module.tianapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "tianapi_import_run")
@NoArgsConstructor
public class TianApiImportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_type", length = 32)
    private String triggerType;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "running";

    @Column(name = "mode", length = 64)
    private String mode;

    @Column(name = "configured_calls_per_category")
    private Integer configuredCallsPerCategory;

    @Column(name = "configured_api_calls_per_category")
    private Integer configuredApiCallsPerCategory;

    @Column(name = "estimated_max_http_calls")
    private Integer estimatedMaxHttpCalls;

    @Column(name = "total_imported", nullable = false)
    private Integer totalImported = 0;

    @Column(name = "total_skipped", nullable = false)
    private Integer totalSkipped = 0;

    @Column(name = "total_calls", nullable = false)
    private Integer totalCalls = 0;

    @Column(name = "total_kb_imported", nullable = false)
    private Integer totalKbImported = 0;

    @Column(name = "skip_reason", columnDefinition = "TEXT")
    private String skipReason;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Timestamp startedAt;

    @Column(name = "finished_at")
    private Timestamp finishedAt;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (startedAt == null) startedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
