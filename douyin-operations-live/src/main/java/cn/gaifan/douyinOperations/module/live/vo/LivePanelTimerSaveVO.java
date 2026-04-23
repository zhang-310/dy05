package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 直播提词器倒计时持久化（Redis）入参
 */
@Data
public class LivePanelTimerSaveVO {

    @NotNull
    private Long liveSessionId;

    @NotNull
    private Long slotId;

    @NotNull
    private Integer slotIndex;

    @NotNull
    private Integer durationSeconds;

    @NotNull
    private Boolean running;

    /** 运行中：客户端根据 remaining 计算的绝对截止时间（毫秒，System.currentTimeMillis） */
    private Long deadlineEpochMs;

    /** 已暂停：剩余秒数 */
    private Integer pausedRemainingSeconds;
}
