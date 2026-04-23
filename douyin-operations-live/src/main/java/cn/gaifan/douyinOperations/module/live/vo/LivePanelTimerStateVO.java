package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 直播提词器倒计时 Redis 状态（与 {@link LivePanelTimerSaveVO} 字段对齐）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LivePanelTimerStateVO {

    private Long liveSessionId;
    private Long slotId;
    private Integer slotIndex;
    private Integer durationSeconds;
    private Boolean running;
    private Long deadlineEpochMs;
    private Integer pausedRemainingSeconds;
}
