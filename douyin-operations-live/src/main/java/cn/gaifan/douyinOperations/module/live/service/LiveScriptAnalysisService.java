package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.SimilarityItemVO;
import cn.gaifan.douyinOperations.module.live.vo.SkeletonSlotVO;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 直播话术分析与交互服务：refine / chat / similarity / skeleton / suggestImprovement
 * 从 LiveAiService 拆分，专注于对已有话术的分析、修改、交互。
 */
public interface LiveScriptAnalysisService {

    /** 提问式修改话术 */
    String refineScript(Long scriptId, String userQuestion, Long userId, Long modelId);

    /** 段内微调：仅修改指定片段，其余内容不变，返回完整修改后话术 */
    String refineSegment(Long scriptId, String segmentText, String instruction, Long userId, Long modelId);

    /** 提问式修改话术（SSE 流式） */
    void refineScriptStream(Long scriptId, String userQuestion, Long userId, OutputStream out, Long modelId) throws java.io.IOException;

    /** 根据效果评分生成改进版话术 */
    String suggestImprovement(Long scriptId, Long userId, Long modelId);

    /** AI 写作助手 */
    String chatForScript(Long scriptId, String userMessage, Long userId, Long modelId);

    /** AI 写作助手（SSE 流式） */
    void chatForScriptStream(Long scriptId, String userMessage, Long userId, OutputStream out, Long modelId) throws java.io.IOException;

    /** 批量应用同一指令到多段话术 */
    Map<Long, String> batchChatForScript(List<Long> scriptIds, String userMessage, Long userId, Long modelId);

    /** 相似度检测 */
    List<SimilarityItemVO> checkSimilarity(Long sessionId, Long userId);

    /** 骨架生成 */
    List<SkeletonSlotVO> generateSkeleton(Long sessionId, Long userId, Long modelId);

    /** 骨架生成（SSE 流式） */
    void generateSkeletonStream(Long sessionId, Long userId, OutputStream out, Long modelId) throws java.io.IOException;
}
