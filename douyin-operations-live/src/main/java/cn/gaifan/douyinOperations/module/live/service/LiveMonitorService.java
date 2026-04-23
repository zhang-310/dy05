package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.*;

/**
 * 直播监控数据服务接口
 */
public interface LiveMonitorService {

    /**
     * 搜索直播监控数据列表
     */
    PageResultVO<LiveMonitorVO> search(LiveMonitorSearchVO vo);

    /**
     * 保存直播监控数据
     */
    long save(LiveMonitorVO vo);

    /**
     * 根据直播场次查询监控数据
     */
    java.util.List<LiveMonitorVO> getBySessionId(Long sessionId);

    /**
     * 删除直播场次的所有监控数据
     */
    void deleteBySessionId(Long sessionId);

    /** 获取当前话术段 */
    java.util.Map<String, Object> getCurrentSlot(Long sessionId);

    /** 获取场次下所有话术段列表 */
    java.util.List<java.util.Map<String, Object>> getScriptSlots(Long sessionId);

    /** 获取场次实时指标 */
    java.util.Map<String, Object> getRealtimeMetrics(Long sessionId);

    /** 跳转到下一话术段 */
    java.util.Map<String, Object> nextSlot(Long sessionId);

    /** 跳转到指定话术段 */
    java.util.Map<String, Object> skipToSlot(Long sessionId, int slotIndex);
}
