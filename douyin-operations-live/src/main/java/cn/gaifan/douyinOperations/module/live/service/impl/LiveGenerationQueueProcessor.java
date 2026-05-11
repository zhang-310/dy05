package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LiveFullGenerationProgressTracker;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationTaskService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 异步执行一键话术生成（G-2），与 {@code LiveScriptGenerationController#generateFullSse} 计费与归因对齐。
 */
@Service
public class LiveGenerationQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(LiveGenerationQueueProcessor.class);

    @Resource
    private LiveGenerationTaskService generationTaskService;

    @Resource
    private LiveAiService liveAiService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AiCallLogService aiCallLogService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LiveFullGenerationProgressTracker fullGenerationProgressTracker;

    public void processQueuedFullGeneration(long taskId) {
        LiveGenerationTask task = generationTaskService.getByIdOrThrow(taskId);
        if (!"queued".equals(task.getStatus())) {
            log.debug("skip live generation taskId={} status={}", taskId, task.getStatus());
            return;
        }

        LiveAiGenerateVO vo;
        try {
            vo = objectMapper.readValue(task.getRequestPayload(), LiveAiGenerateVO.class);
        } catch (Exception e) {
            generationTaskService.failTask(taskId, "请求体解析失败: " + e.getMessage());
            throw new RuntimeException(e);
        }

        Long userId = task.getOwnerId();
        Long sessionId = vo.getSessionId();
        fullGenerationProgressTracker.markSession(sessionId);
        generationTaskService.startTask(taskId);

        long start = System.currentTimeMillis();
        try {
            LiveAiFullResultVO full = liveAiService.generateFullWithProgress(vo, new LiveAiService.FullGenerateProgressCallback() {
                @Override
                public void onProgress(int current, int total, String slotType) {
                    if (current == 0 && total > 0) {
                        try {
                            generationTaskService.setTotalSlotsIfUnset(taskId, total);
                        } catch (Exception e) { log.debug("setTotalSlotsIfUnset失败 taskId={}: {}", taskId, e.getMessage()); }
                    }
                }

                @Override
                public void onSlotDone(Long scriptId, String content, String scriptType, String slotLabel, Integer sequenceNo, Integer index) {
                    try {
                        generationTaskService.onSlotCompleted(taskId);
                    } catch (Exception e) { log.debug("onSlotCompleted失败 taskId={}: {}", taskId, e.getMessage()); }
                }

                @Override
                public void onSlotFailed(Long scriptId, String slotLabel, String errorMsg, Integer index) {
                    try {
                        generationTaskService.onSlotFailed(taskId);
                    } catch (Exception e) { log.warn("onSlotFailed失败 taskId={}: {}", taskId, e.getMessage()); }
                }
            });

            aiQuotaService.consume(userId, full.getConsumption());
            String refChunks = full.getReferencedChunkIds();
            AiCallLogService.LogEntry entry = new AiCallLogService.LogEntry(userId, "live_script_full", null, "llm", null, null, null, null,
                    System.currentTimeMillis() - start, 1, null, false, refChunks, null);
            Long callLogId = aiCallLogService.log(entry);
            if (full.getSessionIdForAttribution() != null) {
                aiCallLogService.linkToPublish(callLogId, null, full.getSessionIdForAttribution(), userId);
            }
            if (full.getScriptIdsForAttribution() != null) {
                for (Long sid : full.getScriptIdsForAttribution()) {
                    liveAiService.attachAiCallLogToScript(sid, callLogId);
                }
            }
            generationTaskService.completeTask(taskId);
            log.info("live async full generation done taskId={} sessionId={} scripts={}", taskId, sessionId,
                    full.getResults() != null ? full.getResults().size() : 0);
        } catch (Exception e) {
            log.warn("live async full generation failed taskId={}: {}", taskId, e.getMessage());
            generationTaskService.failTask(taskId, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, "live_script_full", null, "llm", null, null, null, null,
                    System.currentTimeMillis() - start, 0, e.getMessage(), false, null, null));
            throw new RuntimeException("live_script_full async failed taskId=" + taskId, e);
        } finally {
            fullGenerationProgressTracker.unmarkSession(sessionId);
        }
    }
}
