package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class AbVariantSaveVO {
    private Long id;
    @NotNull(message = "实验 ID 不能为空")
    private Long experimentId;
    @NotBlank(message = "变体名称不能为空")
    private String variantName;
    @NotBlank(message = "变体类型不能为空")
    private String variantType;
    private String content;
    private String entityType;
    private Long entityId;
    /** 话术风格编码（script_style 实验时必填） */
    private String styleCode;
}
