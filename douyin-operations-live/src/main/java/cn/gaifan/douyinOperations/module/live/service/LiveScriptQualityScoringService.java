package cn.gaifan.douyinOperations.module.live.service;

import java.util.Map;

/**
 * 话术质量评分服务（合规+流畅+吸引力）
 */
public interface LiveScriptQualityScoringService {

    /**
     * 对指定话术进行质量评分
     * @param scriptId 话术 ID
     * @param ownerId 用户 ID
     * @return 评分结果
     */
    Map<String, Object> scoreScript(Long scriptId, Long ownerId);

    /**
     * 对场次下所有话术进行批量质量评分
     * @param sessionId 场次 ID
     * @param ownerId 用户 ID
     */
    void scoreSession(Long sessionId, Long ownerId);
}
