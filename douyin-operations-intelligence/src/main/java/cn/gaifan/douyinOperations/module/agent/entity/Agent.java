package cn.gaifan.douyinOperations.module.agent.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "agent")
@SQLRestriction("deleted = 0")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "agent_name", nullable = false, length = 128)
    private String agentName;

    @Column(name = "description", length = 512)
    private String description;

    /**
     * 智能体类型：
     * 0=自定义
     * 1=话术生成（内容生成）
     * 2=违规检测（合规检测）
     * 3=商品分析（数据分析）
     * 4=场次规划（策略规划）
     * 5=数据分析
     * 6=客户服务
     */
    @Column(name = "agent_type", nullable = false)
    private Integer agentType;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "model_config", columnDefinition = "TEXT")
    private String modelConfig;

    /** 可用工具列表（JSON 数组，如 ["kb_rag_search","product_search"]） */
    @Column(name = "available_tools", columnDefinition = "TEXT")
    private String availableTools;

    /** 评分统计：评论总数 */
    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    /** 评分统计：评分总和 */
    @Column(name = "rating_sum", nullable = false)
    private Integer ratingSum = 0;

    /** 使用次数（对话次数），用于市场热度排序 */
    @Column(name = "conversation_count", nullable = false)
    private Integer conversationCount = 0;

    /** 响应模式：1=即时 2=异步 */
    @Column(name = "response_mode", nullable = false)
    private Integer responseMode = 1;

    /** 状态：0=禁用 1=启用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "version", nullable = false)
    private Integer version = 0;

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
