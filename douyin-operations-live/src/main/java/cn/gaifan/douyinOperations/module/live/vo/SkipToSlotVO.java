package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 跳转到指定话术段请求 VO
 * P1-9: 替换 Map 参数，添加校验
 */
@Data
public class SkipToSlotVO {

    @NotNull(message = "slotIndex 不能为空")
    @Min(value = 0, message = "slotIndex 必须大于等于 0")
    private Integer slotIndex;
}
