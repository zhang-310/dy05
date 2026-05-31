package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识源保存入参
 */
@Data
public class KnowledgeSourceSaveVO {

    private Long id;

    @NotBlank(message = "知识源名称不能为空")
    @Size(max = 128)
    private String sourceName;

    @NotBlank(message = "知识源路径不能为空")
    @Size(max = 512)
    private String sourcePath;

    @Size(max = 32)
    private String sourceType = "local";

    private Integer status = 1;
}
