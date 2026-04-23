package cn.gaifan.douyinOperations.module.log.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 系统日志表，与 sql/log/schema.sql 中 sys_system_log 对应
 */
@Data
@Entity
@Table(name = "sys_system_log")
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module", length = 64)
    private String module;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "summary", length = 256)
    private String summary;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 结果 0失败 1成功 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
