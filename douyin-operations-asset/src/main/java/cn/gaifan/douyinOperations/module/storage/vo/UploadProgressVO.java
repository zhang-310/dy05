package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadProgressVO {
    private String uploadId;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Long uploadedBytes;
    private Long fileSize;
    private Integer progressPercent;
    private String status;
}
