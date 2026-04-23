package cn.gaifan.douyinOperations.module.attribution.service;

import cn.gaifan.douyinOperations.module.attribution.vo.AttributionTriggerVO;

import java.util.List;
import java.util.Map;

public interface AttributionService {

    /** 触发归因分析（异步） */
    long triggerAttribution(AttributionTriggerVO vo, Long ownerId);

    /** 获取场次的归因结果 */
    List<Map<String, Object>> getBySessionId(Long sessionId);

    /** 获取归因详情 */
    Map<String, Object> getById(Long id);

    /** 获取归因汇总 */
    Map<String, Object> getSummary(Long sessionId);

    /** 删除归因数据 */
    void deleteBySessionId(Long sessionId);
}
