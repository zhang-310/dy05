package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class UploadInitVO {
    @NotBlank(message = "原始文件名不能为空")
    private String originalFilename;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @NotBlank(message = "文件 MD5 不能为空")
    private String fileMd5;

    @NotBlank(message = "存储 key 不能为空")
    private String storageKey;

    private String module;
}
