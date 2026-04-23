package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ScriptTemplateSaveVO {
    private Long id;
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    private String templateType;
    private String scene;
    @NotBlank(message = "模板内容不能为空")
    private String content;
    private String description;
    private Long userId;
    private Integer status;
}
