package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDigitalHumanTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDigitalHumanTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.DigitalHumanPollingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 数字人合成轮询服务 — 定时查询 HeyGen/D-ID 异步任务状态（Webhook 未到时的 fallback）
 */
@Slf4j
@Service
public class DigitalHumanPollingServiceImpl implements DigitalHumanPollingService {

    @Autowired
    private SvDigitalHumanTaskRepository taskRepository;

    @Value("${app.shortvideo.digital-human.heygen-api-key:}")
    private String heygenApiKey;

    @Value("${app.shortvideo.digital-human.heygen-api-base:https://api.heygen.com}")
    private String heygenApiBase;

    @Value("${app.shortvideo.digital-human.did-api-key:}")
    private String didApiKey;

    @Value("${app.shortvideo.digital-human.did-api-base:https://api.d-id.com}")
    private String didApiBase;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void pollPendingTasks() {
        List<SvDigitalHumanTask> pendingTasks = taskRepository
                .findByStatusInAndDeletedAndRetryCountLessThan(
                        List.of("SUBMITTED", "PROCESSING"), 0, 10);

        if (pendingTasks.isEmpty()) return;
        log.info("[DigitalHumanPoller] 发现 {} 个待轮询任务", pendingTasks.size());

        for (SvDigitalHumanTask task : pendingTasks) {
            try {
                if ("heygen".equals(task.getProvider())) {
                    pollHeygen(task);
                } else if ("did".equals(task.getProvider())) {
                    pollDid(task);
                }
                task.setRetryCount(task.getRetryCount() + 1);
                taskRepository.save(task);
            } catch (Exception e) {
                log.warn("[DigitalHumanPoller] 轮询失败: taskId={}, err={}", task.getId(), e.getMessage());
                task.setRetryCount(task.getRetryCount() + 1);
                taskRepository.save(task);
            }
        }
    }

    private void pollHeygen(SvDigitalHumanTask task) {
        if (!StringUtils.hasText(heygenApiKey)) {
            log.debug("[DigitalHumanPoller] HeyGen API key 未配置，跳过轮询");
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", heygenApiKey);
            HttpEntity<Void> req = new HttpEntity<>(headers);

            String url = heygenApiBase + "/v1/video_status.get?video_id=" + task.getExternalTaskId();
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                JsonNode data = root.path("data");
                String status = data.path("status").asText("");

                if ("completed".equalsIgnoreCase(status)) {
                    task.setStatus("COMPLETED");
                    task.setVideoUrl(data.path("video_url").asText(""));
                    task.setResultJson(resp.getBody());
                    log.info("[DigitalHumanPoller] HeyGen 任务完成: taskId={}, videoUrl={}", task.getId(), task.getVideoUrl());
                    onTaskCompleted(task);
                } else if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                    task.setStatus("FAILED");
                    task.setErrorMessage(data.path("error").asText("unknown error"));
                    task.setResultJson(resp.getBody());
                } else {
                    task.setStatus("PROCESSING");
                }
            }
        } catch (Exception e) {
            log.warn("[DigitalHumanPoller] HeyGen 轮询异常: {}", e.getMessage());
        }
    }

    private void pollDid(SvDigitalHumanTask task) {
        if (!StringUtils.hasText(didApiKey)) {
            log.debug("[DigitalHumanPoller] D-ID API key 未配置，跳过轮询");
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(didApiKey, "");
            HttpEntity<Void> req = new HttpEntity<>(headers);

            String url = didApiBase + "/talks/" + task.getExternalTaskId();
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                String status = root.path("status").asText("");

                if ("done".equalsIgnoreCase(status)) {
                    task.setStatus("COMPLETED");
                    task.setVideoUrl(root.path("result_url").asText(""));
                    task.setResultJson(resp.getBody());
                    log.info("[DigitalHumanPoller] D-ID 任务完成: taskId={}, videoUrl={}", task.getId(), task.getVideoUrl());
                    onTaskCompleted(task);
                } else if ("error".equalsIgnoreCase(status)) {
                    task.setStatus("FAILED");
                    task.setErrorMessage(root.path("error").path("description").asText("unknown error"));
                    task.setResultJson(resp.getBody());
                } else {
                    task.setStatus("PROCESSING");
                }
            }
        } catch (Exception e) {
            log.warn("[DigitalHumanPoller] D-ID 轮询异常: {}", e.getMessage());
        }
    }

    @Override
    public void onTaskCompleted(SvDigitalHumanTask task) {
        log.info("[DigitalHumanPoller] 任务完成回调: taskId={}, projectId={}, videoUrl={}",
                task.getId(), task.getProjectId(), task.getVideoUrl());
        // Future: trigger workflow next step via WorkflowExecutionService
    }
}
