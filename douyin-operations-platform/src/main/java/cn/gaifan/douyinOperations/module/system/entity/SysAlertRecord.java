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
@Table(name = "sys_alert_record")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SysAlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_name", length = 128)
    private String ruleName;

    @Column(name = "metric_name", nullable = false, length = 128)
    private String metricName;

    @Column(nullable = false, length = 512)
    private String message;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(name = "metric_value")
    private Double value;

    private Double threshold;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
        if (deleted == null) deleted = 0;
    }
}
