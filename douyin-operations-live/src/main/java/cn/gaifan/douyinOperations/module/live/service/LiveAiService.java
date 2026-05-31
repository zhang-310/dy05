package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.EmotionalScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptResultVO;
import cn.gaifan.douyinOperations.module.live.vo.SimilarityItemVO;
import cn.gaifan.douyinOperations.module.live.vo.SkeletonSlotVO;

import java.util.List;
import java.util.Map;

/**
 * 直播 AI 话术生成服务
 */
public interface LiveAiService {

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

    /**
     * 生成完整话术流程（带进度回调，用于 SSE 推送）
     * @param vo 生成参数
     * @param progressCallback 每完成一个槽位调用 (current, total, slotType)
     * @return 最终结果（含消费系数）
     */
    LiveAiFullResultVO generateFullWithProgress(LiveAiGenerateVO vo, FullGenerateProgressCallback progressCallback);

    @FunctionalInterface
    interface FullGenerateProgressCallback {
        void onProgress(int current, int total, String slotType);

        default void onSlotDone(Long scriptId, String content, String scriptType, String slotLabel, Integer sequenceNo, Integer index) {}

        default void onSlotFailed(Long scriptId, String slotLabel, String errorMsg, Integer index) {}
    }

    /** 对已有话术进行违规检测（scope=live） */
    LiveAiResultVO.ViolationCheckResult checkViolation(Long userId, Long scriptId);

    /** 按内容进行违规检测（scope=live），用于 AI 生成内容未落库时的检测 */
    LiveAiResultVO.ViolationCheckResult checkViolationByContent(String content, Long userId);

    /** 生成产品 AI 话术（按人设+风格） */
    ProductScriptResultVO generateProductScript(ProductScriptGenerateVO vo, Long userId);

    /** 根据用户提问修改话术（提问式 AI 修改） */
    String refineScript(Long scriptId, String userQuestion, Long userId);

    /** 根据用户提问修改话术（支持指定模型） */
    default String refineScript(Long scriptId, String userQuestion, Long userId, Long modelId) {
        return refineScript(scriptId, userQuestion, userId);
    }

    /** 流式修改话术（SSE 输出） */
    default void refineScriptStream(Long scriptId, String userQuestion, Long userId, java.io.OutputStream out, Long modelId) {}

    /** 片段级修改（支持指令类型） */
    default String refineSegment(Long scriptId, String segmentText, String instruction, Long userId, Long modelId) {
        String prompt = "请只修改下面选中的片段，并把修改后的片段自然合回原话术。"
                + "修改要求：" + instruction + "\n\n选中片段：\n" + segmentText;
        return refineScript(scriptId, prompt, userId, modelId);
    }

    /** 建议改进（生成改进提示） */
    default String suggestImprovement(Long scriptId, Long userId, Long modelId) {
        return refineScript(scriptId, "请分析该话术的优缺点，给出改进建议", userId, modelId);
    }

    /** 将 AI 调用日志关联到话术（用于溯源） */
    default void attachAiCallLogToScript(Long scriptId, Long callLogId) {}

    /** 按槽位需求生成单段话术（不保存，供 AI 分析师自动填充；requirement/durationLimitSec 可覆盖槽位值） */
    String generateForSlot(Long scriptId, String requirement, Integer durationLimitSec);

    /** 按槽位需求生成单段话术（支持指定模型） */
    default String generateForSlot(Long scriptId, String requirement, Integer durationLimitSec, Long modelId) {
        return generateForSlot(scriptId, requirement, durationLimitSec);
    }

    /** 流式生成单段话术（SSE 输出到 OutputStream） */
    default void generateForSlotStream(Long scriptId, String requirement, Integer durationLimitSec, Long modelId, java.io.OutputStream out) {}

    /** 流水线式整场生成（与 generateFullWithProgress 不同，支持更多控制参数） */
    default LiveAiFullResultVO generateFullPipelined(LiveAiGenerateVO vo, FullGenerateProgressCallback callback) {
        return generateFullWithProgress(vo, callback);
    }

    /** 并行批量生成话术，返回 scriptId -> 结果 */
    default java.util.Map<Long, cn.gaifan.douyinOperations.module.live.vo.ParallelGenerateResult> generateParallel(java.util.List<Long> scriptIds, Long modelId) {
        return java.util.Collections.emptyMap();
    }

    /** 流式骨架生成（SSE 输出到 OutputStream） */
    default void generateSkeletonStream(Long sessionId, Long userId, java.io.OutputStream out, Long modelId) {}

    /** AI 写作助手：根据用户提问生成/修改话术（对话式，供编辑时右侧对话框） */
    String chatForScript(Long scriptId, String userMessage, Long userId);

    /** AI 写作助手（支持指定模型） */
    default String chatForScript(Long scriptId, String userMessage, Long userId, Long modelId) {
        return chatForScript(scriptId, userMessage, userId);
    }

    /** AI 写作助手流式输出 */
    default void chatForScriptStream(Long scriptId, String userMessage, Long userId, java.io.OutputStream out, Long modelId) {}

    /** 批量应用同一指令到多段话术，返回 scriptId -> 生成内容 */
    Map<Long, String> batchChatForScript(List<Long> scriptIds, String userMessage, Long userId);

    /** 相似度检测：分析多段话术，找出相似度过高的段落并给出差异化建议 */
    List<SimilarityItemVO> checkSimilarity(Long sessionId, Long userId);

    /** 骨架生成：为每段话术生成一句话摘要和建议时长 */
    List<SkeletonSlotVO> generateSkeleton(Long sessionId, Long userId);

    /** 骨架生成（支持指定模型） */
    default List<SkeletonSlotVO> generateSkeleton(Long sessionId, Long userId, Long modelId) {
        return generateSkeleton(sessionId, userId);
    }

    /** 生成情绪价值话术（名言金句/歇后语/心理鸡汤/女性视角话题） */
    LiveAiResultVO generateEmotionalScript(EmotionalScriptGenerateVO vo, Long userId);
}
