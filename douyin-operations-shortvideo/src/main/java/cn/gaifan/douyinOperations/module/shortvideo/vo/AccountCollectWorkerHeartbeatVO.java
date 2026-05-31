package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class AccountCollectWorkerHeartbeatVO {
    private Long taskId;
    private String workerId;
    private String workerRegion;
    private Integer leaseSeconds;
}
