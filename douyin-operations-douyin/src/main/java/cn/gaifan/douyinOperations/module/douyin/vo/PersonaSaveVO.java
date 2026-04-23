package cn.gaifan.douyinOperations.module.douyin.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 人设保存 VO
 */
@Data
public class PersonaSaveVO {
    private Long id;
    private Long accountId;

    @NotBlank(message = "人设名称不能为空")
    @Size(max = 64, message = "人设名称最长64字符")
    private String personaName;

    @Size(max = 32, message = "人设类型最长32字符")
    private String personaType;  // knowledge / entertainment / lifestyle / commerce

    @Size(max = 512, message = "描述最长512字符")
    private String description;

    @Size(max = 32, message = "语气最长32字符")
    private String tone;  // professional / casual / humorous / warm

    @Size(max = 128, message = "目标受众最长128字符")
    private String targetAudience;

    @Size(max = 128, message = "内容风格最长128字符")
    private String contentStyle;

    @Size(max = 256, message = "关键词最长256字符")
    private String keywords;

    private Integer isDefault;
}
