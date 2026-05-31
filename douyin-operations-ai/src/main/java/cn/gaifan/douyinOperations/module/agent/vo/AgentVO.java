package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 智能体返回值 VO
 * 用于查询智能体时返回的数据结构
 *
 * @author gaifan
 */
@Data
public class AgentVO {
    /** 智能体 ID */
    private Long id;

    /** 智能体名称 */
    private String agentName;

    /** 智能体类型：0=自定义 1=话术生成 2=违规检测 3=商品分析 4=场次规划 5=数据分析 6=客户服务 */
    private Integer agentType;

    /** 系统提示词 */
    private String systemPrompt;

    /** 模型配置（JSON 格式） */
    private String modelConfig;

    /** 响应模式：1=即时 2=异步 */
    private Integer responseMode;

    /** 智能体描述 */
    private String description;

    /** 版本号（支持乐观锁） */
    private Integer version;

    /** 创建时间 */
    private Timestamp createdAt;

    /** 更新时间 */
    private Timestamp updatedAt;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 可用工具列表（JSON 数组） */
    private String availableTools;

    /** 评分统计：评论总数 */
    private Integer ratingCount;

    /** 评分统计：平均评分（四舍五入一位小数） */
    private Double averageRating;

    /** 使用次数（对话次数），用于市场热度排序 */
    private Integer conversationCount;
}

