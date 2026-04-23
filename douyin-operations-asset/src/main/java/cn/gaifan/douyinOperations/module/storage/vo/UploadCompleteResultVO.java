package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadCompleteResultVO {
    private String uploadId;
    private String fileUrl;
    private String storageKey;
    private Long fileSize;
}
