package cn.gaifan.douyinOperations.module.shortvideo.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * LF-05：爆款视频深度分析（下载 → ASR → 抽帧视觉 → LLM 综合拆解）
 */
public interface ViralVideoDeepAnalysisService {

    /**
     * 提交异步深度分析任务，立即返回 taskId（与 viralVideoId 相同）与状态。
     */
    Map<String, Object> startDeepAnalyze(Long viralVideoId, Long userId);

    /**
     * 提交异步深度分析并通过 SSE 推送 {@code progress} / {@code done} / {@code error}（路径须含 {@code -stream}）。
     */
    void startDeepAnalyzeStream(Long viralVideoId, Long userId, SseEmitter emitter);

    /**
     * 轮询深度分析状态与结果摘要。
     */
    Map<String, Object> getDeepAnalyzeStatus(Long viralVideoId, Long userId);

    /**
     * 批量提交深度分析（单请求最多 30 条）。
     */
    List<Map<String, Object>> batchStartDeepAnalyze(List<Long> viralVideoIds, Long userId);

    /**
     * 批量查询深度分析状态（单请求最多 30 条）。
     */
    List<Map<String, Object>> batchGetDeepAnalyzeStatus(List<Long> viralVideoIds, Long userId);

    /**
     * 仅 ASR：提取口播文案并写入 {@code transcript}
     */
    String extractTranscript(Long viralVideoId, Long userId);

    /**
     * 仅抽帧视觉描述
     */
    String extractSceneDescriptions(Long viralVideoId, Long userId);

    /**
     * 重跑多轮深度分析中的某一文本轮次（需结果中含 {@code _roundsRaw}）。
     */
    Map<String, Object> retryDeepAnalyzeRound(Long viralVideoId, Long userId, String roundKey);
}
