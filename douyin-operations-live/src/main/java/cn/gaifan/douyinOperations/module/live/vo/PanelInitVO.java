package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 直播实时面板初始化响应
 * 包含所有话术段落和实时数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanelInitVO {

    /**
     * 直播场次 ID
     */
    private Long liveSessionId;

    /**
     * 所有话术段落列表
     */
    private List<LiveSessionScriptSlotVO> slots;

    /**
     * 当前话术段落序号
     */
    private Integer currentSlotIndex;

    /**
     * 实时数据
     */
    private LiveSessionRealtimeDataVO realtimeData;
}
