package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.LivePanelTimerSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LivePanelTimerStateVO;

import java.util.Optional;

/**
 * 直播实时面板提词器倒计时 Redis 持久化
 */
public interface LivePanelTimerRedisService {

    void save(Long userId, LivePanelTimerSaveVO vo);

    Optional<LivePanelTimerStateVO> load(Long userId, Long liveSessionId);
}
