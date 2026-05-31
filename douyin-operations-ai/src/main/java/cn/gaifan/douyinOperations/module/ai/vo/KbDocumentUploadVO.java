package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库文档上传入参
 */
@Data
public class KbDocumentUploadVO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 256)
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 32)
    private String fileType = "text";

    /** general=通用分块 | script=话术分块 | auto=自动检测 */
    @Size(max = 16)
    private String contentType;
}
