package cn.gaifan.douyinOperations.common.compliance.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 违规规则实体
 * 对应表: compliance_rule
 */
@Data
@Entity
@Table(name = "compliance_rule")
public class ComplianceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 规则编码（唯一标识）
     * 如: LIVE_CONTENT_POLITICAL
     */
    @Column(name = "rule_code", nullable = false, unique = true, length = 64)
    private String ruleCode;

    /**
     * 分类: live=直播, video=短视频, material=素材
     */
    @Column(name = "category", nullable = false, length = 32)
    private String category;

    /**
     * 子分类: content=内容, behavior=行为, product=商品
     */
    @Column(name = "sub_category", length = 32)
    private String subCategory;

    /**
     * 规则名称
     */
    @Column(name = "rule_name", nullable = false, length = 128)
    private String ruleName;

    /**
     * 规则描述
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * 严重程度: critical=严重, high=高, medium=中, low=低
     */
    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    /**
     * 处罚措施
     */
    @Column(name = "punishment", columnDefinition = "TEXT")
    private String punishment;

    /**
     * 违规示例（JSON 数组）
     */
    @Column(name = "examples", columnDefinition = "TEXT")
    private String examples;

    /**
     * 关键词列表（JSON 数组）
     */
    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    /**
     * 正则表达式列表（JSON 数组）
     */
    @Column(name = "patterns", columnDefinition = "TEXT")
    private String patterns;

    /**
     * 规则描述的向量表示（1536 维）
     * 用于语义检索
     */
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding;

    /**
     * 来源 URL
     */
    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    /**
     * 生效日期
     */
    @Column(name = "effective_date")
    private java.sql.Date effectiveDate;

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
