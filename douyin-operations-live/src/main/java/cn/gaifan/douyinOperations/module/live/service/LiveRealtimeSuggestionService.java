package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.LiveSessionRealtimeDataVO;
import cn.gaifan.douyinOperations.module.live.vo.RealtimeSuggestionVO;

import java.util.List;

/**
 * 直播实时建议服务（P2-1 从 LiveRealtimePanelServiceImpl 拆分）
 * 负责根据实时数据评估并生成建议
 */
public interface LiveRealtimeSuggestionService {

    /**
     * 根据实时数据评估并生成建议
     *
     * @param data          实时数据
     * @param liveSessionId 直播场次 ID
     * @return 建议列表，无建议时返回空列表
     */
    List<RealtimeSuggestionVO> evaluateSuggestions(LiveSessionRealtimeDataVO data, Long liveSessionId);
}
