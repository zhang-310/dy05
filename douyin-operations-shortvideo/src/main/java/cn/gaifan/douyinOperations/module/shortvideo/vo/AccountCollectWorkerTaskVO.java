package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class AccountCollectWorkerTaskVO {
    private Long taskId;
    private Long ownerId;
    private Long accountId;
    private Long svAccountId;
    private String inputType;
    private String originalInput;
    private String accountUrl;
    private String accountName;
    private String secUid;
    private Integer maxCount;
    private Long targetKbId;
    private String workerId;
    private String workerRegion;
}
