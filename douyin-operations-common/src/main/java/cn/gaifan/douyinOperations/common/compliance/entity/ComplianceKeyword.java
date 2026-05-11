package cn.gaifan.douyinOperations.common.compliance.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 敏感词库实体
 * 对应表: compliance_keyword
 */
@Data
@Entity
@Table(name = "compliance_keyword")
@SQLRestriction("deleted = 0")
public class ComplianceKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 敏感词
     */
    @Column(name = "keyword", nullable = false, length = 128)
    private String keyword;

    /**
     * 分类: political=政治, porn=色情, violence=暴力, fraud=欺诈, divert=引流, induce=诱导, banned=禁售
     */
    @Column(name = "category", nullable = false, length = 32)
    private String category;

    /**
     * 严重程度: critical=严重, high=高, medium=中, low=低
     */
    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    /**
     * 替换词（可选）
     */
    @Column(name = "replacement", length = 128)
    private String replacement;

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
