package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.SkeletonSlotVO;

import java.io.OutputStream;
import java.util.List;

/**
 * 直播话术骨架生成服务：为场次话术槽位生成摘要与建议时长。
 * 从 LiveScriptAnalysisService 拆分，专注于骨架相关逻辑。
 */
public interface LiveScriptSkeletonService {

    /** 骨架生成（同步） */
    List<SkeletonSlotVO> generateSkeleton(Long sessionId, Long userId, Long modelId);

    /** 骨架生成（SSE 流式） */
    void generateSkeletonStream(Long sessionId, Long userId, OutputStream out, Long modelId) throws java.io.IOException;
}
