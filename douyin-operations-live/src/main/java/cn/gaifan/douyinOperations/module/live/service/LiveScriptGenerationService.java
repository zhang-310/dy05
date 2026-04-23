package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.EmotionalScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptResultVO;

import java.io.OutputStream;

/**
 * 直播话术生成服务：从 LiveAiService 拆分，专注于话术的 AI 生成逻辑。
 */
public interface LiveScriptGenerationService {

    /** 生成开场话术 */
    LiveAiResultVO generateOpening(LiveAiGenerateVO vo);

    /** 生成商品介绍话术 */
    LiveAiResultVO generateProduct(LiveAiGenerateVO vo);

    /** 生成过渡话术 */
    LiveAiResultVO generateTransition(LiveAiGenerateVO vo);

    /** 生成收尾话术 */
    LiveAiResultVO generateClosing(LiveAiGenerateVO vo);

    /** 生成完整话术流程（返回含消费系数，供配额细粒度计费） */
    LiveAiFullResultVO generateFull(LiveAiGenerateVO vo);

    /** 生成完整话术流程（带进度回调，用于 SSE 推送） */
    LiveAiFullResultVO generateFullWithProgress(LiveAiGenerateVO vo, LiveAiService.FullGenerateProgressCallback progressCallback);

    /** 按槽位需求生成单段话术（不保存，供 AI 分析师自动填充）；modelId 可选 */
    String generateForSlot(Long scriptId, String requirement, Integer durationLimitSec, Long modelId);

    /** 按槽位需求生成单段话术（SSE 流式）；modelId 可选 */
    void generateForSlotStream(Long scriptId, String requirement, Integer durationLimitSec, Long modelId, OutputStream out) throws java.io.IOException;

    /** 对已有话术进行违规检测（scope=live） */
    LiveAiResultVO.ViolationCheckResult checkViolation(Long userId, Long scriptId);

    /** 按内容进行违规检测（scope=live） */
    LiveAiResultVO.ViolationCheckResult checkViolationByContent(String content, Long userId);

    /** 生成产品 AI 话术（按人设+风格） */
    ProductScriptResultVO generateProductScript(ProductScriptGenerateVO vo, Long userId);

    /** 生成情绪价值话术 */
    LiveAiResultVO generateEmotionalScript(EmotionalScriptGenerateVO vo, Long userId);

    /** 归因闭环：将 ai_call_log_id 回填到话术记录 */
    void attachAiCallLogToScript(Long scriptId, Long callLogId);

    /** G-1 多商品批量并行生成：为多个槽位或商品并行生成话术，返回成功/失败结果 */
    java.util.Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult> generateParallel(java.util.List<Long> scriptIds, Long modelId);

    /**
     * 分组流水线全场生成（性能优化版）
     * <p>
     * 策略：开场串行生成 → 所有商品槽并行生成 → 过渡/收尾串行生成 → 成篇优化
     * 相比 {@link #generateFullWithProgress} 的串行方案，典型场次预计提速 3-5x。
     * RAG 查询在生成开始前批量执行，避免 N 次独立检索。
     *
     * @param vo               生成请求（含 sessionId、modelId、风格等）
     * @param progressCallback 进度回调（可为 null）
     * @return 全场生成结果
     */
    LiveAiFullResultVO generateFullPipelined(LiveAiGenerateVO vo, LiveAiService.FullGenerateProgressCallback progressCallback);
}
