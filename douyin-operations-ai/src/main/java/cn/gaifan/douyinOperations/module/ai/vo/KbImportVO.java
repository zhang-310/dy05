package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库从路径导入入参
 */
@Data
public class KbImportVO {

    @NotBlank(message = "sourcePath 不能为空")
    @Size(max = 1024)
    private String sourcePath;

    private Long kbId;

    @Size(max = 128)
    private String kbName;

    private Boolean autoClassify = false;
}
