package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.*;

import java.util.List;

/**
 * 直播数据同步服务
 */
public interface LiveDataSyncService {

    LiveSessionDataVO getSessionData(Long sessionId);

    LiveSessionDataVO saveSessionData(LiveSessionDataSaveVO vo);

    List<LiveProductDataVO> getProductDataBySession(Long sessionId);

    LiveProductDataVO saveProductData(LiveProductDataSaveVO vo);

    /** 从 live_monitor 汇总数据到 live_session_data */
    LiveSessionDataVO syncSessionData(Long sessionId);

    /** 历史数据对比：最近 N 场已结束直播（按 endTime 倒序）。days>0 时按最近 N 天过滤 */
    java.util.List<LiveHistoryItemVO> getHistory(Long userId, String roleCode, Long accountId, int limit, Integer days);

    /** 场次数据 + 上一场数据（环比用） */
    SessionDataWithCompareVO getSessionDataWithCompare(Long sessionId);
}
