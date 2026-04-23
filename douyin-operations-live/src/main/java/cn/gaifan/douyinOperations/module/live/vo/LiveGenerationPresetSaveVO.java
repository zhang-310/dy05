package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 生成配置预设保存 VO
 */
@Data
public class LiveGenerationPresetSaveVO {

    /** 为空=新建，非空=更新 */
    private Long id;

    @NotBlank(message = "预设名称不能为空")
    @Size(max = 100, message = "预设名称不能超过 100 个字符")
    private String name;

    private String description;

    @Size(max = 50, message = "风格不能超过 50 个字符")
    private String style;

    private Long modelId;

    private Boolean useKbRef;

    @Size(max = 20, message = "时长模式不能超过 20 个字符")
    private String durationMode;

    /** 热门关键词（JSON 数组或逗号分隔） */
    private String hotKeywords;

    private String ipType;
    private String materialType;
    private String scriptModule;
    private String retentionStrategy;
    private String interactionLevel;

    private Boolean isDefault;
}
