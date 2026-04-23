package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 话术骨架槽位
 */
@Data
public class SkeletonSlotVO {
    private Long scriptId;
    private String scriptType;
    private String summary;
    private Integer suggestedDurationSec;
}
