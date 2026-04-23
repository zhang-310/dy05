package cn.gaifan.douyinOperations.module.copy.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CopyTemplateSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;
    private String category;
    private String description;
    private Integer status;
}
