package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "ai_prompt_template")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiPromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 模板归属所有者 ID（0=系统内置） */
    @Column(name = "owner_id")
    private Long ownerId;

    /** 模板唯一编码（如 live_opening_v1） */
    @Column(name = "template_code", length = 128)
    private String templateCode;

    /** 变体名（如 default / aggressive / gentle） */
    @Column(name = "variant_name", length = 64)
    private String variantName = "default";

    /** 版本号（用于 A/B 测试和迭代追踪） */
    @Column(name = "version", length = 32)
    private String version = "1.0";

    /** 激活状态：0=禁用 1=激活 */
    @Column(name = "is_active", nullable = false)
    private Integer isActive = 1;

    /** 是否默认模板：0=否 1=是 */
    @Column(name = "is_default", nullable = false)
    private Integer isDefault = 0;

    /** 系统提示词 */
    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    /** 用户侧提示词模板（支持 {{variable}} 占位符） */
    @Column(name = "user_prompt_tpl", columnDefinition = "TEXT")
    private String userPromptTpl;

    /** 模型偏好提示（如 deepseek / ollama） */
    @Column(name = "model_hint", length = 64)
    private String modelHint;

    /** LLM temperature 配置（0-2） */
    @Column(name = "temperature")
    private Float temperature;

    /** LLM max_tokens 配置 */
    @Column(name = "max_tokens")
    private Integer maxTokens;

    /** 使用次数 */
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    /** 最后使用时间 */
    @Column(name = "last_used_at")
    private Timestamp lastUsedAt;

    /** 标签（JSON 数组，如 ["直播","护肤","爆款"]） */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    /** 平均效果分（自优化引擎反馈） */
    @Column(name = "avg_score")
    private Float avgScore;

    /** P50 效果分 */
    @Column(name = "p50_score")
    private Float p50Score;

    /** P90 效果分 */
    @Column(name = "p90_score")
    private Float p90Score;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    private String templateContent;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;

    /** 状态：0=不可用 1=可用 */
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
