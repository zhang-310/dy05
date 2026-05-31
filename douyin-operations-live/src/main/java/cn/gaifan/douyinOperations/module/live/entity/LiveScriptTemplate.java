package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 直播话术模板表
 * 高效话术（effectiveness_score > 80）自动入库，供下次直播复用
 */
@Getter
@Setter
@Entity
@Table(name = "live_script_template")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveScriptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    /** 模板内容（兼容旧字段） */
    @Column(name = "template_content", columnDefinition = "TEXT")
    private String templateContent;

    /** 模板归属所有者 ID（0=公共） */
    @Column(name = "owner_id")
    private Long ownerId;

    /** 是否自动采集（0=手动 1=系统自动入库） */
    @Column(name = "auto_collected")
    private Integer autoCollected = 0;

    @Column(name = "script_type", nullable = false, length = 32)
    private String scriptType;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "variables", length = 512)
    private String variables;

    @Column(name = "effectiveness_score", precision = 5, scale = 2)
    private BigDecimal effectivenessScore = BigDecimal.ZERO;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "avg_conversion_rate", precision = 5, scale = 2)
    private BigDecimal avgConversionRate = BigDecimal.ZERO;

    @Column(name = "source_script_id")
    private Long sourceScriptId;

    @Column(name = "source_session_id")
    private Long sourceSessionId;

    /** P3-04 行业模板扩展：行业编码（cosmetics/food/clothing/jewelry/digital 等），null=通用模板 */
    @Column(name = "industry_code", length = 64)
    private String industryCode;

    /** 是否为系统预置模板（0=用户创建, 1=系统预置行业模板） */
    @Column(name = "is_preset")
    private Integer isPreset = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

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
