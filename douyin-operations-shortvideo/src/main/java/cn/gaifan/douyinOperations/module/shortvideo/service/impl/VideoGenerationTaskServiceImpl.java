package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.config.VideoGenerationTaskAmqpConfig;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoGenerationTask;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWebhookDlq;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoGenerationTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvWebhookDlqRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoMaterialService;
import cn.gaifan.douyinOperations.module.shortvideo.service.VideoGenerationTaskService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvWebhookDlqVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图生视频异步任务服务实现 (Phase 2.2)
 */
@Service
public class VideoGenerationTaskServiceImpl implements VideoGenerationTaskService {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationTaskServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private SvVideoGenerationTaskRepository taskRepository;
    @Resource
    private SvWebhookDlqRepository webhookDlqRepository;
    @Resource
    private ShortVideoMaterialService materialService;
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Override
    public Long submitTask(Long ownerId, Long projectId, Long shotListId, List<Map<String, Object>> keyframes,
                          String quality, String aspectRatio) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (keyframes == null || keyframes.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "keyframes 不能为空");

        Map<String, Object> request = Map.of(
            "projectId", projectId != null ? projectId : 0,
            "shotListId", shotListId != null ? shotListId : 0,
            "quality", quality != null ? quality : "",
            "aspectRatio", aspectRatio != null ? aspectRatio : "9:16",
            "keyframes", keyframes
        );
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "序列化失败");
        }

        SvVideoGenerationTask task = new SvVideoGenerationTask();
        task.setOwnerId(ownerId);
        task.setProjectId(projectId);
        task.setShotListId(shotListId);
        task.setRequestJson(requestJson);
        task.setStatus("pending");
        task.setProgressTotal(keyframes.size());
        task = taskRepository.save(task);

        if (rabbitTemplate != null) {
            try {
                rabbitTemplate.convertAndSend(VideoGenerationTaskAmqpConfig.QUEUE_VIDEO_GENERATION, String.valueOf(task.getId()));
            } catch (Exception e) {
                log.warn("图生视频任务 MQ 发布失败: {}", e.getMessage());
                task.setStatus("failed");
                task.setErrorMessage("MQ 发布失败: " + e.getMessage());
                taskRepository.save(task);
            }
        } else {
            task.setStatus("failed");
            task.setErrorMessage("RabbitMQ 未配置");
            taskRepository.save(task);
        }
        return task.getId();
    }

    @Override
    public PageResultVO<Map<String, Object>> listTasks(Long ownerId, int page, int rows, Long projectId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        int p = Math.max(0, page);
        int r = Math.min(Math.max(rows, 1), 100);
        Pageable pageable = PageRequest.of(p, r);
        Page<SvVideoGenerationTask> pg = projectId != null && projectId > 0
                ? taskRepository.findByOwnerIdAndProjectIdOrderByCreateTimeDesc(ownerId, projectId, pageable)
                : taskRepository.findByOwnerIdOrderByCreateTimeDesc(ownerId, pageable);
        List<Map<String, Object>> list = pg.getContent().stream().map(this::toTaskListRow).toList();
        return PageResultVO.of(pg.getTotalElements(), list, p, r);
    }

    private Map<String, Object> toTaskListRow(SvVideoGenerationTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("taskType", "img2video");
        m.put("status", t.getStatus());
        int total = t.getProgressTotal() != null && t.getProgressTotal() > 0 ? t.getProgressTotal() : 0;
        int cur = t.getProgressCurrent() != null ? t.getProgressCurrent() : 0;
        int pct = 0;
        if ("completed".equals(t.getStatus())) {
            pct = 100;
        } else if (total > 0) {
            pct = (int) Math.min(100, Math.round(100.0 * cur / total));
        }
        m.put("progress", pct);
        m.put("projectId", t.getProjectId());
        m.put("shotListId", t.getShotListId());
        m.put("createTime", t.getCreateTime() != null ? t.getCreateTime().toString() : "");
        m.put("errorMessage", t.getErrorMessage());
        return m;
    }

    @Override
    public TaskStatusVO getStatus(Long taskId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (taskId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");

        SvVideoGenerationTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");

        List<ShortVideoMaterialService.VideoResult> videos = null;
        if (task.getResultJson() != null && !task.getResultJson().isEmpty()) {
            try {
                videos = objectMapper.readValue(task.getResultJson(), new TypeReference<List<ShortVideoMaterialService.VideoResult>>() {});
            } catch (Exception ignored) {}
        }

        String message = task.getStatus();
        if ("processing".equals(task.getStatus())) {
            message = "生成中 " + (task.getProgressCurrent() != null ? task.getProgressCurrent() : 0) + "/" + (task.getProgressTotal() != null ? task.getProgressTotal() : 0);
        } else if ("completed".equals(task.getStatus())) {
            message = "已完成";
        } else if ("failed".equals(task.getStatus())) {
            message = task.getErrorMessage() != null ? task.getErrorMessage() : "生成失败";
        }

        return new TaskStatusVO(
            task.getId(),
            task.getStatus(),
            task.getProgressCurrent() != null ? task.getProgressCurrent() : 0,
            task.getProgressTotal() != null ? task.getProgressTotal() : 0,
            message,
            videos,
            task.getErrorMessage()
        );
    }

    @Override
    public PageResultVO<SvWebhookDlqVO> listWebhookDlq(Long ownerId, BasicQueryDto query) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        int page = query != null && query.getPage() != null ? query.getPage() : 0;
        int rows = query != null && query.getRows() != null ? Math.min(Math.max(query.getRows(), 1), 1000) : 30;
        Pageable pageable = PageRequest.of(page, rows);
        Page<SvWebhookDlq> pg = webhookDlqRepository.findByOwnerIdOrderByIdDesc(ownerId, pageable);
        List<SvWebhookDlqVO> list = pg.getContent().stream().map(this::toSvWebhookDlqVO).toList();
        return PageResultVO.of(pg.getTotalElements(), list, page, rows);
    }

    private SvWebhookDlqVO toSvWebhookDlqVO(SvWebhookDlq e) {
        SvWebhookDlqVO vo = new SvWebhookDlqVO();
        vo.setId(e.getId());
        if (e.getCreateTime() != null) {
            vo.setCreateTimeMs(e.getCreateTime().getTime());
        }
        vo.setTaskId(e.getTaskId());
        String sha = e.getWebhookUrlSha256();
        if (sha != null && sha.length() > 12) {
            vo.setWebhookUrlFingerprint(sha.substring(0, 12) + "…");
        } else {
            vo.setWebhookUrlFingerprint(sha);
        }
        vo.setLastHttpStatus(e.getLastHttpStatus());
        vo.setAttemptCount(e.getAttemptCount());
        vo.setErrorPreview(e.getErrorPreview());
        vo.setEventCode(e.getEventCode());
        return vo;
    }

    @Override
    public void cancelTask(Long taskId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvVideoGenerationTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        if (!"pending".equals(task.getStatus())) throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "仅 pending 状态可取消");
        task.setStatus("cancelled");
        taskRepository.save(task);
    }

    @Override
    public Long retryTask(Long taskId, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvVideoGenerationTask old = taskRepository.findById(taskId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在"));
        if (!old.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        if (!"failed".equals(old.getStatus())) throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "仅 failed 状态可重试");

        try {
            Map<String, Object> req = objectMapper.readValue(old.getRequestJson(), new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> keyframes = (List<Map<String, Object>>) req.get("keyframes");
            Long projectId = req.get("projectId") instanceof Number n ? n.longValue() : null;
            Long shotListId = req.get("shotListId") instanceof Number n ? n.longValue() : null;
            String quality = req.get("quality") instanceof String s ? s : null;
            String aspectRatio = req.get("aspectRatio") instanceof String s ? s : null;
            if (projectId != null && projectId == 0) projectId = null;
            if (shotListId != null && shotListId == 0) shotListId = null;
            return submitTask(ownerId, projectId, shotListId, keyframes, quality, aspectRatio);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "重试失败: " + e.getMessage());
        }
    }

    /** 供 Consumer 调用：执行任务 */
    public void processTask(Long taskId) {
        SvVideoGenerationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !"pending".equals(task.getStatus())) return;

        task.setStatus("processing");
        taskRepository.save(task);

        try {
            Map<String, Object> req = objectMapper.readValue(task.getRequestJson(), new TypeReference<Map<String, Object>>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> keyframes = (List<Map<String, Object>>) req.get("keyframes");
            Long projectId = req.get("projectId") instanceof Number n ? n.longValue() : null;
            Long shotListId = req.get("shotListId") instanceof Number n ? n.longValue() : null;
            String quality = req.get("quality") instanceof String s ? s : null;
            String aspectRatio = req.get("aspectRatio") instanceof String s ? s : null;
            if (projectId != null && projectId == 0) projectId = null;
            if (shotListId != null && shotListId == 0) shotListId = null;

            List<ShortVideoMaterialService.Img2VideoInput> inputs = keyframes.stream()
                .map(m -> new ShortVideoMaterialService.Img2VideoInput(
                    m.get("shotId") instanceof Number n ? n.longValue() : null,
                    m.get("shotNumber") instanceof Number n ? n.intValue() : null,
                    m.get("imageUrl") instanceof String s ? s : "",
                    m.get("endFrameUrl") instanceof String s ? s : null,
                    m.get("duration") instanceof Number n ? n.intValue() : 5,
                    m.get("motion") instanceof String s ? s : "zoom-in",
                    m.get("sceneDescription") instanceof String s ? s : null,
                    m.get("quality") instanceof String s ? s : quality,
                    m.get("aspectRatio") instanceof String s ? s : aspectRatio,
                    m.get("cameraType") instanceof String s ? s : null,
                    m.get("mood") instanceof String s ? s : null,
                    m.get("action") instanceof String s ? s : null))
                .toList();

            List<ShortVideoMaterialService.VideoResult> videos = materialService.img2videoBatchWithProgress(
                projectId, shotListId, inputs, task.getOwnerId(),
                evt -> {
                    task.setProgressCurrent(evt.current());
                    task.setProgressTotal(evt.total());
                    taskRepository.save(task);
                });

            task.setStatus("completed");
            task.setProgressCurrent(videos.size());
            task.setProgressTotal(videos.size());
            task.setResultJson(objectMapper.writeValueAsString(videos));
            task.setErrorMessage(null);
        } catch (Exception e) {
            log.warn("图生视频任务执行失败 taskId={}: {}", taskId, e.getMessage());
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage() != null ? e.getMessage() : "生成失败");
        }
        taskRepository.save(task);
    }
}
