package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * R-6：单场次在大屏对比行内的摘要 + 实时指标
 */
@Data
public class LiveMultiSessionRowVO {

    private Long liveSessionId;
    private String liveTitle;
    /** 与 live_session.status 一致：0 计划中 1 直播中 2 已结束 3 已取消 */
    private Integer sessionStatus;
    private LiveSessionRealtimeDataVO realtimeData;
}
