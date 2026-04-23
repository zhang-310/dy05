package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UploadCompleteVO {
    @NotBlank(message = "上传 ID 不能为空")
    private String uploadId;
}
