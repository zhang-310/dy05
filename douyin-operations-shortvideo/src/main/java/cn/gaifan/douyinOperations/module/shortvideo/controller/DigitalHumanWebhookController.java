package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDigitalHumanTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDigitalHumanTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数字人合成回调接收 — HeyGen / D-ID Webhook
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/short-video/webhooks")
public class DigitalHumanWebhookController {

    @Autowired
    private SvDigitalHumanTaskRepository taskRepository;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.shortvideo.service.DigitalHumanPollingService pollingService;

    /**
     * HeyGen 回调
     * HeyGen sends: { "event_type": "avatar_video.success", "data": { "video_id": "...", "video_url": "..." } }
     */
    @PostMapping("/heygen")
    public RESTResult<String> heygenWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[Webhook] HeyGen 回调: {}", payload);
        try {
            String eventType = String.valueOf(payload.getOrDefault("event_type", ""));
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.getOrDefault("data", Map.of());
            String videoId = String.valueOf(data.getOrDefault("video_id", ""));

            var taskOpt = taskRepository.findByProviderAndExternalTaskIdAndDeleted("heygen", videoId, 0);
            if (taskOpt.isEmpty()) {
                log.warn("[Webhook] HeyGen 回调找不到对应任务: videoId={}", videoId);
                return RESTResult.success("ignored");
            }

            SvDigitalHumanTask task = taskOpt.get();
            if ("avatar_video.success".equals(eventType)) {
                String videoUrl = String.valueOf(data.getOrDefault("video_url", ""));
                task.setStatus("COMPLETED");
                task.setVideoUrl(videoUrl);
                task.setResultJson(payload.toString());
            } else if (eventType.contains("fail") || eventType.contains("error")) {
                task.setStatus("FAILED");
                task.setErrorMessage(String.valueOf(data.getOrDefault("error", eventType)));
                task.setResultJson(payload.toString());
            } else {
                task.setStatus("PROCESSING");
            }
            taskRepository.save(task);

            if ("COMPLETED".equals(task.getStatus()) && pollingService != null) {
                pollingService.onTaskCompleted(task);
            }

            return RESTResult.success("ok");
        } catch (Exception e) {
            log.error("[Webhook] HeyGen 回调处理失败", e);
            return RESTResult.success("error");
        }
    }

    /**
     * D-ID 回调
     * D-ID sends: { "id": "...", "status": "done", "result_url": "..." }
     */
    @PostMapping("/did")
    public RESTResult<String> didWebhook(@RequestBody Map<String, Object> payload) {
        log.info("[Webhook] D-ID 回调: {}", payload);
        try {
            String talkId = String.valueOf(payload.getOrDefault("id", ""));
            String status = String.valueOf(payload.getOrDefault("status", ""));

            var taskOpt = taskRepository.findByProviderAndExternalTaskIdAndDeleted("did", talkId, 0);
            if (taskOpt.isEmpty()) {
                log.warn("[Webhook] D-ID 回调找不到对应任务: talkId={}", talkId);
                return RESTResult.success("ignored");
            }

            SvDigitalHumanTask task = taskOpt.get();
            if ("done".equals(status)) {
                String resultUrl = String.valueOf(payload.getOrDefault("result_url", ""));
                task.setStatus("COMPLETED");
                task.setVideoUrl(resultUrl);
                task.setResultJson(payload.toString());
            } else if ("error".equals(status)) {
                task.setStatus("FAILED");
                task.setErrorMessage(String.valueOf(payload.getOrDefault("error", "unknown")));
                task.setResultJson(payload.toString());
            } else {
                task.setStatus("PROCESSING");
            }
            taskRepository.save(task);

            if ("COMPLETED".equals(task.getStatus()) && pollingService != null) {
                pollingService.onTaskCompleted(task);
            }

            return RESTResult.success("ok");
        } catch (Exception e) {
            log.error("[Webhook] D-ID 回调处理失败", e);
            return RESTResult.success("error");
        }
    }
}
