package cn.gaifan.douyinOperations.module.system.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 数据同步日志表，与 sql/system/schema.sql 中 sys_sync_log 一一对应
 * 注意：无 deleted 字段，日志通过定时任务物理清理
 */
@Data
@Entity
@Table(name = "sys_sync_log")
public class SysSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_type", nullable = false, length = 64)
    private String syncType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "running";

    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "fail_count")
    private Integer failCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "start_time")
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (startTime == null) startTime = new Timestamp(System.currentTimeMillis());
    }
}
