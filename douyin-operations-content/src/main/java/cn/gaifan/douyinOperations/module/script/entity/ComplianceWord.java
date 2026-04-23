package cn.gaifan.douyinOperations.module.script.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 合规词库：绝对化用语、医疗功效词
 * 表：sc_compliance_word
 */
@Data
@Entity
@Table(name = "sc_compliance_word")
@SQLRestriction("deleted = 0")
public class ComplianceWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** absolute=绝对化用语(可自动修复) medical=医疗功效(不可修复) */
    @Column(name = "word_type", nullable = false, length = 16)
    private String wordType;

    @Column(name = "word_value", nullable = false, length = 128)
    private String wordValue;

    @Column(name = "replacement", length = 256)
    private String replacement;

    @Column(name = "is_enabled", nullable = false)
    private Integer isEnabled = 1;

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
