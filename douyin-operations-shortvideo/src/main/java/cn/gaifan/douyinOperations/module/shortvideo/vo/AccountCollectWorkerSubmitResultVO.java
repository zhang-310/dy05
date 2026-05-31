package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class AccountCollectWorkerSubmitResultVO {
    private Long taskId;
    private Integer submittedVideos;
    private Integer createdVideos;
    private String status;
}
