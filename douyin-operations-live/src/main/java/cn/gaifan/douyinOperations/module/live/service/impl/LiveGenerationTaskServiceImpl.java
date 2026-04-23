package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import cn.gaifan.douyinOperations.module.live.repository.LiveGenerationTaskRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveGenerationTaskService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationTaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 话术生成任务服务实现
 */
@Service
public class LiveGenerationTaskServiceImpl implements LiveGenerationTaskService {

    private static final Logger log = LoggerFactory.getLogger(LiveGenerationTaskServiceImpl.class);

    /** 超过此时间仍为 running/pending 的任务视为残留，自动标记为 failed（30 分钟） */
    private static final long STALE_TASK_MILLIS = 30 * 60 * 1000L;

    @Resource
    private LiveGenerationTaskRepository liveGenerationTaskRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    @Transactional
    public LiveGenerationTask createTask(Long sessionId, Long ownerId, Integer totalSlots,
                                          String style, Long modelId, Boolean useKbRef,
                                          List<String> hotKeywords) {
        LiveGenerationTask task = new LiveGenerationTask();
        task.setSessionId(sessionId);
        task.setOwnerId(ownerId);
        task.setTotalSlots(totalSlots != null ? totalSlots : 0);
        task.setStyle(style);
        task.setModelId(modelId);
        task.setUseKbRef(useKbRef != null ? useKbRef : false);
        task.setStatus("pending");
        task.setCompletedSlots(0);
        task.setFailedSlots(0);
        if (hotKeywords != null && !hotKeywords.isEmpty()) {
            try {
                task.setHotKeywords(OBJECT_MAPPER.writeValueAsString(hotKeywords));
            } catch (JsonProcessingException ignored) {
                // fallback: store as comma-separated
                task.setHotKeywords(String.join(",", hotKeywords));
            }
        }
        return liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public LiveGenerationTask createQueuedFullGenerationTask(Long ownerId, String requestPayloadJson, LiveAiGenerateVO vo) {
        if (vo == null || vo.getSessionId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "异步生成缺少场次 ID");
        }
        if (requestPayloadJson == null || requestPayloadJson.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "异步生成缺少请求体");
        }
        LiveGenerationTask task = new LiveGenerationTask();
        task.setSessionId(vo.getSessionId());
        task.setOwnerId(ownerId);
        task.setTotalSlots(0);
        task.setStyle(vo.getStyle());
        task.setModelId(vo.getModelId());
        task.setUseKbRef(vo.getUseKbRef() != null ? vo.getUseKbRef() : Boolean.TRUE);
        if (vo.getHotKeywords() != null && !vo.getHotKeywords().isEmpty()) {
            try {
                task.setHotKeywords(OBJECT_MAPPER.writeValueAsString(vo.getHotKeywords()));
            } catch (JsonProcessingException ignored) {
                task.setHotKeywords(String.join(",", vo.getHotKeywords()));
            }
        }
        task.setRequestPayload(requestPayloadJson);
        task.setExecutionMode("rabbit");
        task.setStatus("queued");
        task.setCompletedSlots(0);
        task.setFailedSlots(0);
        return liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void startTask(Long taskId) {
        LiveGenerationTask task = findTaskOrThrow(taskId);
        task.setStatus("running");
        liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void onSlotCompleted(Long taskId) {
        LiveGenerationTask task = findTaskOrThrow(taskId);
        task.setCompletedSlots(task.getCompletedSlots() + 1);
        liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void onSlotFailed(Long taskId) {
        LiveGenerationTask task = findTaskOrThrow(taskId);
        task.setFailedSlots(task.getFailedSlots() + 1);
        liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void updateProgress(Long taskId, Integer completedSlots, Integer failedSlots) {
        LiveGenerationTask task = findTaskOrThrow(taskId);
        if (completedSlots != null) {
            task.setCompletedSlots(completedSlots);
        }
        if (failedSlots != null) {
            task.setFailedSlots(failedSlots);
        }
        liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void setTotalSlotsIfUnset(Long taskId, int totalSlots) {
        if (totalSlots <= 0) {
            return;
        }
        LiveGenerationTask task = findTaskOrThrow(taskId);
        if (task.getTotalSlots() == null || task.getTotalSlots() == 0) {
            task.setTotalSlots(totalSlots);
            liveGenerationTaskRepository.save(task);
        }
    }

    @Override
    @Transactional
    public void completeTask(Long taskId) {
        LiveGenerationTask task = findTaskOrThrow(taskId);
        task.setStatus("completed");
        liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void failTask(Long taskId, String errorMessage) {
        LiveGenerationTask task = findTaskOrThrow(taskId);
        task.setStatus("failed");
        task.setErrorMessage(errorMessage);
        liveGenerationTaskRepository.save(task);
    }

    @Override
    @Transactional
    public Optional<LiveGenerationTask> getActiveTask(Long sessionId) {
        List<LiveGenerationTask> tasks = liveGenerationTaskRepository
                .findBySessionIdAndStatusIn(sessionId, List.of("running", "pending", "queued"));
        if (tasks.isEmpty()) return Optional.empty();
        // 清理残留任务：超过 30 分钟仍未完成的视为 stale，自动标记为 failed
        long now = System.currentTimeMillis();
        LiveGenerationTask active = null;
        for (LiveGenerationTask task : tasks) {
            Timestamp ut = task.getUpdateTime() != null ? task.getUpdateTime() : task.getCreateTime();
            if (ut != null && now - ut.getTime() > STALE_TASK_MILLIS) {
                log.warn("清理残留生成任务: taskId={} sessionId={} status={} updateTime={}",
                        task.getId(), task.getSessionId(), task.getStatus(), ut);
                task.setStatus("failed");
                task.setErrorMessage("任务超时（超过30分钟未完成），已自动标记失败");
                liveGenerationTaskRepository.save(task);
            } else if (active == null) {
                active = task;
            }
        }
        return Optional.ofNullable(active);
    }

    @Override
    public LiveGenerationTask getByIdOrThrow(Long taskId) {
        return findTaskOrThrow(taskId);
    }

    @Override
    public LiveGenerationTaskVO getLatestBySession(Long sessionId) {
        LiveGenerationTask task = liveGenerationTaskRepository
                .findTopBySessionIdOrderByCreateTimeDesc(sessionId);
        return task != null ? LiveGenerationTaskVO.fromEntity(task) : null;
    }

    private LiveGenerationTask findTaskOrThrow(Long taskId) {
        return liveGenerationTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_GENERATION_TASK_NOT_FOUND,
                        "生成任务不存在: " + taskId));
    }
}
