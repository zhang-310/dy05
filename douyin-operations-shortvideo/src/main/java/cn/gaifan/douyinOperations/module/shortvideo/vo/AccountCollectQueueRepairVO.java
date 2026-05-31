package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class AccountCollectQueueRepairVO {

    private Integer releasedExpired;
    private Integer terminalExpired;
    private Integer retriedFailed;

    public Integer getTotalAffected() {
        return safe(releasedExpired) + safe(terminalExpired) + safe(retriedFailed);
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
