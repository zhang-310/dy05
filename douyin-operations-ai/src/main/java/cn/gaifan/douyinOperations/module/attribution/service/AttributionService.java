package cn.gaifan.douyinOperations.module.attribution.service;

import cn.gaifan.douyinOperations.module.attribution.vo.AttributionTriggerVO;

import java.util.List;
import java.util.Map;

public interface AttributionService {

    /** 触发归因分析（异步） */
    long triggerAttribution(AttributionTriggerVO vo, Long ownerId);

    /** 获取场次的归因结果 */
    List<Map<String, Object>> getBySessionId(Long sessionId);

    // P0-1: 带所有权校验的 getBySessionId
    List<Map<String, Object>> getBySessionId(Long sessionId, Long userId);

    /** 获取归因详情 */
    Map<String, Object> getById(Long id);

    // P0-1: 带所有权校验的 getById
    Map<String, Object> getById(Long id, Long userId);

    /** 获取归因汇总 */
    Map<String, Object> getSummary(Long sessionId);

    // P0-1: 带所有权校验的 getSummary
    Map<String, Object> getSummary(Long sessionId, Long userId);

    /** 删除归因数据 */
    void deleteBySessionId(Long sessionId);

    // P0-1: 带所有权校验的 deleteBySessionId
    void deleteBySessionId(Long sessionId, Long userId);
}
