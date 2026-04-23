package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Prompt 模板保存 VO
 */
@Data
public class BenchmarkPromptTemplateSaveVO {

    /**
     * ID（更新时必填）
     */
    private Long id;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String templateName;

    /**
     * 模板编码
     */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码长度不能超过50")
    private String templateCode;

    /**
     * 模板内容
     */
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    /**
     * 模板变量（JSON 格式）
     */
    private String templateVariables;

    /**
     * 场景类型
     */
    @Size(max = 50, message = "场景类型长度不能超过50")
    private String sceneType;

    /**
     * 行业分类
     */
    @Size(max = 50, message = "行业分类长度不能超过50")
    private String industry;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 描述
     */
    private String description;
}
