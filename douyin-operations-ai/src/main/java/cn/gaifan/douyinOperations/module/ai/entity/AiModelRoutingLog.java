package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * 模型路由日志：记录每次路由决策的结果，用于历史学习调整权重
 */
@Getter
@Setter
@Entity
@Table(name = "ai_model_routing_log")
public class AiModelRoutingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 内容类型（portrait / panorama / action / vfx / realistic / anime / default） */
    @Column(name = "content_type", length = 30)
    private String contentType;

    /** 选择的模型标识 */
    @Column(name = "selected_model", length = 100)
    private String selectedModel;

    /** 延迟（毫秒） */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /** 质量评分 */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /** 是否成功 */
    @Column(name = "success")
    private Boolean success;

    /** Token 消耗 */
    @Column(name = "cost_tokens")
    private Integer costTokens;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    protected void onCreate() {
        if (this.createTime == null) this.createTime = new Timestamp(System.currentTimeMillis());
    }
}
