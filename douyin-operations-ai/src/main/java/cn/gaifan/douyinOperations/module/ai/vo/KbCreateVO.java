package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库创建入参
 */
@Data
public class KbCreateVO {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128)
    private String name;

    @Size(max = 512)
    private String description;
}
