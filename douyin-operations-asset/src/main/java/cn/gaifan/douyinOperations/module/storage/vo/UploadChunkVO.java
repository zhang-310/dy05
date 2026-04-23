package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UploadChunkVO {
    @NotBlank(message = "上传 ID 不能为空")
    private String uploadId;

    @NotNull(message = "分块序号不能为空")
    private Integer chunkIndex;

    @NotBlank(message = "分块 MD5 不能为空")
    private String chunkMd5;
}
