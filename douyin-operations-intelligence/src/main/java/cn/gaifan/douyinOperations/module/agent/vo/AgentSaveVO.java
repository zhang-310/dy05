package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

/**
 * 智能体保存 VO
 * 用于创建或更新智能体时的入参
 *
 * @author gaifan
 */
@Data
public class AgentSaveVO {
    /** 智能体 ID（新增时为空，更新时必填） */
    private Long id;

    /** 智能体名称 */
    @NotBlank(message = "智能体名称不能为空")
    @Size(min = 2, max = 50, message = "智能体名称长度必须在 2-50 之间")
    private String agentName;

    /** 智能体类型：0=自定义 1=话术生成 2=违规检测 3=商品分析 4=场次规划 5=数据分析 6=客户服务 */
    @NotNull(message = "智能体类型不能为空")
    private Integer agentType;

    /** 系统提示词 */
    @NotBlank(message = "系统提示词不能为空")
    @Size(max = 5000, message = "系统提示词长度不能超过 5000")
    private String systemPrompt;

    /** 模型配置（JSON 格式，可选，包含 modelId 和 parameters） */
    private String modelConfig;

    /** 响应模式：1=即时 2=异步（可选，默认 1） */
    private Integer responseMode;

    /** 智能体描述（可选） */
    private String description;

    /** 可用工具列表（JSON 数组，如 ["kb_rag_search","product_search"]） */
    private String availableTools;
}

