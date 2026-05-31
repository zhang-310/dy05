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
@Table(name = "tianapi_import_category_stat")
@NoArgsConstructor
public class TianApiImportCategoryStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "planned_calls", nullable = false)
    private Integer plannedCalls = 0;

    @Column(name = "calls", nullable = false)
    private Integer calls = 0;

    @Column(name = "imported", nullable = false)
    private Integer imported = 0;

    @Column(name = "skipped", nullable = false)
    private Integer skipped = 0;

    @Column(name = "empty_responses", nullable = false)
    private Integer emptyResponses = 0;

    @Column(name = "failed_calls", nullable = false)
    private Integer failedCalls = 0;

    @Column(name = "quota_exhausted", nullable = false)
    private Boolean quotaExhausted = false;

    @Column(name = "stop_reason", columnDefinition = "TEXT")
    private String stopReason;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
        if (plannedCalls == null) plannedCalls = 0;
        if (calls == null) calls = 0;
        if (imported == null) imported = 0;
        if (skipped == null) skipped = 0;
        if (emptyResponses == null) emptyResponses = 0;
        if (failedCalls == null) failedCalls = 0;
        if (quotaExhausted == null) quotaExhausted = false;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
