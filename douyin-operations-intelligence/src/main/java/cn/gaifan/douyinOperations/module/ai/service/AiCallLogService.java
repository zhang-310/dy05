package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.vo.CallLogSearchVO;

/**
 * AI 调用日志服务：记录每次 AI 调用，支持效果归因
 */
public interface AiCallLogService {

    /**
     * 分页查询调用日志（管理员）
     */
    PageResultVO<AiCallLog> search(CallLogSearchVO vo);

    /**
     * 记录一次 AI 调用
     *
     * @param entry 日志条目
     * @return 日志 ID，可用于后续关联 videoId/sessionId
     */
    Long log(LogEntry entry);

    /**
     * 关联调用记录与发布内容（用户发布后回填）
     *
     * @param callLogId 调用日志 ID
     * @param videoId   短视频 ID（与 sessionId 二选一）
     * @param sessionId 直播场次 ID
     * @param userId    当前用户 ID（用于数据隔离校验）
     */
    void linkToPublish(Long callLogId, Long videoId, Long sessionId, Long userId);

    /** 日志条目构建器 */
    record LogEntry(
            Long userId,
            String callType,
            String templateCode,
            String modelCode,
            String inputSummary,
            Integer outputLength,
            Integer promptTokens,
            Integer completionTokens,
            Long durationMs,
            int status,
            String errorMessage,
            boolean isFallback,
            String referencedChunkIds,
            String stageTimings
    ) {
        public static LogEntry of(Long userId, String callType, String modelCode, long durationMs, int status) {
            return new LogEntry(userId, callType, null, modelCode, null, null, null, null, durationMs, status, null, false, null, null);
        }
    }
}
