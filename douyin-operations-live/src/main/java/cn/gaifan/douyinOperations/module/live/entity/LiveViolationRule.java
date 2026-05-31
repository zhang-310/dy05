package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "live_violation_rule")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveViolationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform_id", nullable = false)
    private Long platformId;

    @Column(name = "word", nullable = false, length = 128)
    private String word;

    @Column(name = "level", nullable = false, length = 16)
    private String level = "warning";

    @Column(name = "reason", length = 256)
    private String reason;

    @Column(name = "replacement", length = 256)
    private String replacement;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "active", nullable = false)
    private Integer active = 1;

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
