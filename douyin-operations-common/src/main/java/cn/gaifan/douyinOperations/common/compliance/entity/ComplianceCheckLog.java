package cn.gaifan.douyinOperations.common.compliance.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 违规检测记录实体
 * 对应表: compliance_check_log
 */
@Data
@Entity
@Table(name = "compliance_check_log")
public class ComplianceCheckLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 内容类型: script=话术, video=短视频, material=素材
     */
    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    /**
     * 内容 ID（可选）
     */
    @Column(name = "content_id")
    private Long contentId;

    /**
     * 检测内容
     */
    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    /**
     * 检测结果: pass=通过, warning=警告, reject=拒绝
     */
    @Column(name = "check_result", nullable = false, length = 16)
    private String checkResult;

    /**
     * 匹配的规则列表（JSON 数组）
     */
    @Column(name = "matched_rules", columnDefinition = "TEXT")
    private String matchedRules;

    /**
     * 风险评分（0-100，越高越危险）
     */
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    /**
     * 修改建议
     */
    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    /**
     * 检测耗时（毫秒）
     */
    @Column(name = "check_duration_ms")
    private Integer checkDurationMs;

    /**
     * 检测时间
     */
    @Column(name = "check_time", nullable = false)
    private Timestamp checkTime;

    @PrePersist
    protected void onCreate() {
        checkTime = new Timestamp(System.currentTimeMillis());
    }
}
