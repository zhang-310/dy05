package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热点时间窗口（Phase 3.4）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotspotWindow {
    /** golden / silver / bronze / expired */
    private String windowType;
    /** 剩余有效时间（小时） */
    private int remainingHours;
    private String advice;
}
