package cn.gaifan.douyinOperations.module.storage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadInitResultVO {
    private String uploadId;
    private Boolean isSecondUpload;
    private String secondUploadUrl;
    private Integer chunkSize;
    private Integer totalChunks;
    private List<Integer> uploadedChunks;
}
