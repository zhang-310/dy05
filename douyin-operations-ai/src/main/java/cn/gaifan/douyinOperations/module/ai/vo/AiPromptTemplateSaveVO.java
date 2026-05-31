package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiPromptTemplateSaveVO {

    /** 主键，null 时新增，非 null 时更新 */
    private Long id;

    /** 关联用户 ID */
    private Long userId;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称最长128字符")
    private String templateName;

    /** 模板内容（旧字段，保留兼容） */
    private String templateContent;

    @Size(max = 32, message = "分类最长32字符")
    private String category;

    /** 变量定义（JSON） */
    private String variables;

    /** 状态：0=不可用 1=可用 */
    private Integer status;

    @Size(max = 64, message = "模板编码最长64字符")
    private String templateCode;

    @Size(max = 64, message = "变体名称最长64字符")
    private String variantName;

    private Integer version;

    /** 系统提示词 */
    private String systemPrompt;

    /** 用户提示词模板 */
    private String userPromptTpl;

    @Size(max = 64, message = "模型标识最长64字符")
    private String modelHint;

    private Float temperature;

    private Integer maxTokens;

    private Boolean isActive;

    private Boolean isDefault;

    private Integer usageCount;

    private Float avgScore;

    private Float p50Score;

    private Float p90Score;

    private Long ownerId;
}
