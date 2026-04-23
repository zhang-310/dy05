package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 话术段落操作参数
 * 用于前端发送话术操作请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotOperationVO {

    /**
     * 直播场次 ID
     */
    @NotNull(message = "直播场次 ID 不能为空")
    private Long liveSessionId;

    /**
     * 目标段落序号（跳转时必需）
     */
    private Integer slotIndex;
}
