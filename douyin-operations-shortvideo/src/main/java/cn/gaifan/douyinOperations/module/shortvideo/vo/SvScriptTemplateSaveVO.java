package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class SvScriptTemplateSaveVO {
    private Long id;
    private Long ownerId;
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    private String templateType;
    private String scene;
    private Long categoryId;
    @NotBlank(message = "模板内容不能为空")
    private String content;
    private String description;
    private Integer durationHint;
    private Integer status;
}
