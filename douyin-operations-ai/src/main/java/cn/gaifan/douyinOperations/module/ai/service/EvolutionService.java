package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareRequestVO;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareResultVO;

import java.util.Map;

public interface EvolutionService {

    // ─── 爆款拆解 ────────────────────────────────────────────────

    PageResultVO<Map<String, Object>> searchViralAnalysis(Long ownerId, Integer status, int page, int rows);

    Map<String, Object> getViralAnalysis(Long id, Long ownerId);

    /** 触发爆款拆解任务（异步执行） */
    long triggerViralAnalysis(Long videoId, Long ownerId, Long accountId);

    /**
     * 完成爆款拆解（HTTP：需为记录 owner，或请求头 {@code X-Viral-Callback-Secret} 与配置一致）。
     *
     * @param callbackSecretValid 为 true 时跳过 owner 校验（供自动化/内网回调）
     */
    void completeViralAnalysis(Long id, String report, String successFactors,
                               String replicableMethods, Integer qualityScore,
                               Long tokensUsed, String modelUsed,
                               Long requestUserId, boolean callbackSecretValid);

    void deleteViralAnalysis(Long id, Long ownerId);

    // ─── 视频对比分析 ────────────────────────────────────────────────

    /**
     * 视频对比分析
     * @param vo 对比请求参数
     * @param userId 用户 ID
     * @return 对比分析结果
     */
    VideoCompareResultVO compareVideos(VideoCompareRequestVO vo, Long userId);

    // ─── 直播复盘 ────────────────────────────────────────────────

    PageResultVO<Map<String, Object>> searchLiveReviews(Long ownerId, Integer status, int page, int rows);

    Map<String, Object> getLiveReview(Long id);

    /** 触发直播复盘任务（异步执行） */
    long triggerLiveReview(Long sessionId, Long ownerId, Long accountId);

    /** 完成直播复盘（内部回调） */
    void completeLiveReview(Long id, String report, String topScripts,
                            String weakPoints, Long tokensUsed, String modelUsed);

    void deleteLiveReview(Long id);

    // ─── 索引队列 ────────────────────────────────────────────────

    long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority);

    /** 入队索引（含目标知识库） */
    long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority, Long targetKbId);

    /** 进化引擎统计；ownerId 为当前登录用户，仅统计其爆款拆解与直播复盘（索引队列为全局待处理数） */
    Map<String, Object> getEvolutionStats(Long ownerId);
}
