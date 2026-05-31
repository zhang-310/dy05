package cn.gaifan.douyinOperations.module.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sys_alert_rule")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SysAlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "metric_name", nullable = false, length = 128)
    private String metricName;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false)
    private Double threshold;

    @Column(nullable = false, length = 8)
    private String operator;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (enabled == null) enabled = true;
        if (deleted == null) deleted = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
