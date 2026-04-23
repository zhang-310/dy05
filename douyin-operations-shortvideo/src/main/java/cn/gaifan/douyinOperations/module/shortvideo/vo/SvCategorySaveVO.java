package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class SvCategorySaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long ownerId;
    @NotBlank(message = "分类名称不能为空")
    private String name;
    private String description;
    private Integer sortOrder;
}
