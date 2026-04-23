package cn.gaifan.douyinOperations.module.copy.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CopyLibrarySaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
    @NotBlank(message = "文案标题不能为空")
    private String title;
    @NotBlank(message = "文案内容不能为空")
    private String content;
    private String category;
    private String tags;
    private Integer rating;
    private Integer status;
}
