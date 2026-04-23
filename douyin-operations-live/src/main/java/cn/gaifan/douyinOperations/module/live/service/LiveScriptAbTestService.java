package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptAbTest;

/**
 * 话术版本 A/B 测试服务（Phase 2.5）
 */
public interface LiveScriptAbTestService {

    LiveScriptAbTest createTest(Long scriptId, Long versionAId, Long versionBId, int trafficSplit, Long ownerId);

    void recordImpression(Long testId, String variant);

    void updateMetrics(Long testId, String variant, double conversionRate, double retentionRate, double interactionRate);

    /** 基于 trafficSplit 为 session 分配 A/B 变体（同一 session 内固定） */
    String getVariantForSession(Long testId, Long sessionId);

    /** 计算统计显著性，样本充足时自动判定胜者 */
    void calculateSignificance(Long testId);

    PageResultVO<LiveScriptAbTest> listTests(Long ownerId, int page, int rows);
}
