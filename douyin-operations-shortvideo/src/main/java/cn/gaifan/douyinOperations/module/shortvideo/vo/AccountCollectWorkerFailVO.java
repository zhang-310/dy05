package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class AccountCollectWorkerFailVO {
    private Long taskId;
    private String workerId;
    private String errorMessage;
    private Boolean retryable;
}
