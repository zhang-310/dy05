package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class AccountCollectWorkerClaimVO {
    private String workerId;
    private String workerRegion;
    private Integer leaseSeconds;
}
