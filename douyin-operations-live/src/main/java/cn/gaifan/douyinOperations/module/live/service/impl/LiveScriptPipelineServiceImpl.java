package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptPipeline;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptPipelineRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationTaskService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptPipelineService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 直播话术全自动流水线服务实现
 * 编排流程：Generate → QC → Auto-Refine → Save
 * 通过 LiveGenerationTaskService 异步触发全量生成（Rabbit MQ 队列）
 */
@Service
public class LiveScriptPipelineServiceImpl implements LiveScriptPipelineService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptPipelineServiceImpl.class);

    @Resource
    private LiveScriptPipelineRepository pipelineRepository;

    @Autowired(required = false)
    private LiveGenerationTaskService generationTaskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public LiveScriptPipeline startPipeline(Long sessionId, Long modelId, String style,
                                            Boolean useKbRef, Long ownerId) {
        // 构建配置 JSON
        Map<String, Object> config = new LinkedHashMap<>();
        if (modelId != null) config.put("modelId", modelId);
        if (style != null) config.put("style", style);
        if (useKbRef != null) config.put("useKbRef", useKbRef);

        LiveScriptPipeline pipeline = new LiveScriptPipeline();
        pipeline.setSessionId(sessionId);
        pipeline.setOwnerId(ownerId);
        pipeline.setStatus("pending");
        pipeline.setTotalScripts(0);
        pipeline.setCompletedScripts(0);
        pipeline.setRefinedScripts(0);
        pipeline.setFailedScripts(0);

        try {
            pipeline.setConfigJson(objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            log.warn("Failed to serialize pipeline config", e);
            pipeline.setConfigJson("{}");
        }

        pipeline = pipelineRepository.save(pipeline);
        log.info("Pipeline created: id={}, sessionId={}, ownerId={}", pipeline.getId(), sessionId, ownerId);

        // 异步触发全量生成（通过 Rabbit MQ 队列 Generate → QC → Refine → Save）
        final Long pipelineId = pipeline.getId();
        triggerAsyncGeneration(sessionId, modelId, style, useKbRef, ownerId, pipelineId);

        return pipeline;
    }

    /**
     * 异步触发生成任务（避免阻塞 HTTP 线程）
     * 通过 LiveGenerationTaskService.createQueuedFullGenerationTask 入队到 Rabbit MQ
     */
    @Async
    protected void triggerAsyncGeneration(Long sessionId, Long modelId, String style,
                                          Boolean useKbRef, Long ownerId, Long pipelineId) {
        if (generationTaskService == null) {
            log.warn("[Pipeline] LiveGenerationTaskService 未注入，流水线 {} 无法触发异步生成", pipelineId);
            updatePipelineStatus(pipelineId, "failed");
            return;
        }
        try {
            // 构建 LiveAiGenerateVO 用于触发全量生成队列
            LiveAiGenerateVO vo = new LiveAiGenerateVO();
            vo.setSessionId(sessionId);
            if (modelId != null) vo.setModelId(modelId);
            if (style != null) vo.setStyle(style);
            if (useKbRef != null) vo.setUseKbRef(useKbRef);

            String payload = objectMapper.writeValueAsString(Map.of(
                    "sessionId", sessionId,
                    "pipelineId", pipelineId,
                    "ownerId", ownerId
            ));
            generationTaskService.createQueuedFullGenerationTask(ownerId, payload, vo);
            updatePipelineStatus(pipelineId, "running");
            log.info("[Pipeline] 异步生成已入队: pipelineId={}, sessionId={}", pipelineId, sessionId);
        } catch (Exception e) {
            log.error("[Pipeline] 触发异步生成失败: pipelineId={}, error={}", pipelineId, e.getMessage(), e);
            updatePipelineStatus(pipelineId, "failed");
        }
    }

    @Transactional
    protected void updatePipelineStatus(Long pipelineId, String status) {
        pipelineRepository.findById(pipelineId).ifPresent(p -> {
            p.setStatus(status);
            pipelineRepository.save(p);
        });
    }

    @Override
    public LiveScriptPipeline getPipelineStatus(Long pipelineId, Long ownerId) {
        return pipelineRepository.findByIdAndOwnerIdAndDeleted(pipelineId, ownerId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "流水线不存在"));
    }

    @Override
    @Transactional
    public void cancelPipeline(Long pipelineId, Long ownerId) {
        LiveScriptPipeline pipeline = pipelineRepository.findByIdAndOwnerIdAndDeleted(pipelineId, ownerId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "流水线不存在"));

        String status = pipeline.getStatus();
        if ("completed".equals(status) || "failed".equals(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "流水线已结束，无法取消");
        }

        pipeline.setStatus("failed");
        pipelineRepository.save(pipeline);
        log.info("Pipeline cancelled: id={}", pipelineId);
    }
}
