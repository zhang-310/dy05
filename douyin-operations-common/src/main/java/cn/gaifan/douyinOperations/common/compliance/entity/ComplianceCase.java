package cn.gaifan.douyinOperations.common.compliance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 违规案例库实体
 * 对应表: compliance_case
 */
@Getter
@Setter
@Entity
@Table(name = "compliance_case")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class ComplianceCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联规则 ID
     */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /**
     * 案例标题
     */
    @Column(name = "case_title", nullable = false, length = 256)
    private String caseTitle;

    /**
     * 案例内容（违规内容示例）
     */
    @Column(name = "case_content", nullable = false, columnDefinition = "TEXT")
    private String caseContent;

    /**
     * 违规原因分析
     */
    @Column(name = "violation_reason", columnDefinition = "TEXT")
    private String violationReason;

    /**
     * 实际处罚结果
     */
    @Column(name = "punishment_result", columnDefinition = "TEXT")
    private String punishmentResult;

    /**
     * 案例来源
     */
    @Column(name = "case_source", length = 128)
    private String caseSource;

    /**
     * 案例日期
     */
    @Column(name = "case_date")
    private Date caseDate;

    /**
     * 状态: 1=启用, 0=禁用
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 逻辑删除: 0=未删除, 1=已删除
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    private Timestamp updateTime;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        createTime = now;
        updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
