package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.service.impl.AccountVideoScraper;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerClaimVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerFailVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerHeartbeatVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerSubmitVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectWorkerTaskVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Remote collector mode: this node only runs Playwright collection and posts raw results to the master server.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.shortvideo.account-collect.remote-worker", name = "enabled", havingValue = "true")
public class RemoteAccountCollectWorkerScheduler {

    private final AccountVideoScraper scraper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentMap<Long, Boolean> running = new ConcurrentHashMap<>();

    @Value("${app.shortvideo.account-collect.remote-worker.master-url:}")
    private String masterUrl;

    @Value("${app.shortvideo.account-collect.remote-worker.token:}")
    private String token;

    @Value("${app.shortvideo.account-collect.worker.id:}")
    private String configuredWorkerId;

    @Value("${app.shortvideo.account-collect.worker.region:remote}")
    private String workerRegion;

    @Value("${app.shortvideo.account-collect.worker.lease-seconds:900}")
    private int leaseSeconds;

    private String workerId;

    public RemoteAccountCollectWorkerScheduler(AccountVideoScraper scraper, ObjectMapper objectMapper) {
        this.scraper = scraper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        workerId = StringUtils.hasText(configuredWorkerId) ? configuredWorkerId.trim() : buildDefaultWorkerId();
        log.info("远程账号采集 worker 已启动: workerId={}, region={}, masterUrl={}",
                workerId, workerRegion, masterUrl);
    }

    @Scheduled(fixedDelayString = "${app.shortvideo.account-collect.worker.poll-ms:15000}",
            initialDelayString = "${app.shortvideo.account-collect.worker.initial-delay-ms:10000}")
    public void poll() {
        if (!isConfigured() || !running.isEmpty()) {
            return;
        }
        AccountCollectWorkerTaskVO task = claim();
        if (task == null || task.getTaskId() == null) {
            return;
        }
        running.put(task.getTaskId(), Boolean.TRUE);
        try {
            runTask(task);
        } finally {
            running.remove(task.getTaskId());
        }
    }

    @Scheduled(fixedDelayString = "${app.shortvideo.account-collect.worker.heartbeat-ms:60000}",
            initialDelayString = "${app.shortvideo.account-collect.worker.heartbeat-initial-delay-ms:30000}")
    public void heartbeat() {
        if (!isConfigured()) {
            return;
        }
        if (running.isEmpty()) {
            AccountCollectWorkerHeartbeatVO vo = new AccountCollectWorkerHeartbeatVO();
            vo.setWorkerId(workerId);
            vo.setWorkerRegion(workerRegion);
            vo.setLeaseSeconds(leaseSeconds);
            try {
                post("/api/v1/short-video/account-collect/worker/heartbeat", vo, Object.class);
            } catch (Exception e) {
                log.warn("远程采集空闲心跳失败: workerId={}, err={}", workerId, e.getMessage());
            }
            return;
        }
        for (Long taskId : running.keySet()) {
            AccountCollectWorkerHeartbeatVO vo = new AccountCollectWorkerHeartbeatVO();
            vo.setTaskId(taskId);
            vo.setWorkerId(workerId);
            vo.setWorkerRegion(workerRegion);
            vo.setLeaseSeconds(leaseSeconds);
            try {
                post("/api/v1/short-video/account-collect/worker/heartbeat", vo, Object.class);
            } catch (Exception e) {
                log.warn("远程采集心跳失败: taskId={}, err={}", taskId, e.getMessage());
            }
        }
    }

    private void runTask(AccountCollectWorkerTaskVO task) {
        try {
            AccountVideoScraper.ScrapeResult scraped = scrape(task);
            AccountCollectWorkerSubmitVO submit = new AccountCollectWorkerSubmitVO();
            submit.setTaskId(task.getTaskId());
            submit.setWorkerId(workerId);
            submit.setAccountName(scraped != null ? scraped.getAccountName() : null);
            submit.setSecUid(scraped != null ? scraped.getSecUid() : null);
            submit.setVideos(toSubmitVideos(scraped != null ? scraped.getVideos() : List.of(), task.getMaxCount()));
            post("/api/v1/short-video/account-collect/worker/submit", submit, Object.class);
            log.info("远程采集任务已提交: taskId={}, videos={}", task.getTaskId(), submit.getVideos().size());
        } catch (Exception e) {
            AccountCollectWorkerFailVO fail = new AccountCollectWorkerFailVO();
            fail.setTaskId(task.getTaskId());
            fail.setWorkerId(workerId);
            fail.setErrorMessage(e.getMessage());
            fail.setRetryable(!isNonRetryable(e.getMessage()));
            try {
                post("/api/v1/short-video/account-collect/worker/fail", fail, Object.class);
            } catch (Exception reportEx) {
                log.warn("远程采集失败上报失败: taskId={}, err={}", task.getTaskId(), reportEx.getMessage());
            }
            log.warn("远程采集任务失败: taskId={}, retryable={}, err={}",
                    task.getTaskId(), fail.getRetryable(), e.getMessage());
        }
    }

    private AccountVideoScraper.ScrapeResult scrape(AccountCollectWorkerTaskVO task) {
        if (!scraper.isAvailable()) {
            throw new IllegalStateException("Playwright 不可用");
        }
        if ("search_video".equalsIgnoreCase(task.getInputType())) {
            return scraper.scrapeSearchVideos(task.getAccountUrl(), null);
        }
        return scraper.scrapeAccountVideos(task.getAccountUrl(), null);
    }

    private List<AccountCollectWorkerSubmitVO.CollectedVideoVO> toSubmitVideos(
            List<AccountVideoScraper.ScrapedVideo> scrapedVideos, Integer maxCount) {
        List<AccountCollectWorkerSubmitVO.CollectedVideoVO> result = new ArrayList<>();
        if (scrapedVideos == null || scrapedVideos.isEmpty()) {
            return result;
        }
        int limit = maxCount != null && maxCount > 0 ? Math.min(maxCount, scrapedVideos.size()) : scrapedVideos.size();
        for (int i = 0; i < limit; i++) {
            AccountVideoScraper.ScrapedVideo source = scrapedVideos.get(i);
            AccountCollectWorkerSubmitVO.CollectedVideoVO target = new AccountCollectWorkerSubmitVO.CollectedVideoVO();
            target.setVideoId(source.getVideoId());
            target.setVideoUrl(source.getVideoUrl());
            target.setTitle(source.getTitle());
            target.setCoverUrl(source.getCoverUrl());
            target.setViewCount(source.getViewCount());
            target.setLikeCount(source.getLikeCount());
            target.setCommentCount(source.getCommentCount());
            target.setShareCount(source.getShareCount());
            target.setFavoriteCount(source.getFavoriteCount());
            result.add(target);
        }
        return result;
    }

    private AccountCollectWorkerTaskVO claim() {
        AccountCollectWorkerClaimVO vo = new AccountCollectWorkerClaimVO();
        vo.setWorkerId(workerId);
        vo.setWorkerRegion(workerRegion);
        vo.setLeaseSeconds(leaseSeconds);
        return post("/api/v1/short-video/account-collect/worker/claim", vo, AccountCollectWorkerTaskVO.class);
    }

    private <T> T post(String path, Object body, Class<T> dataType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Collector-Token", token);
        String url = masterUrl.replaceAll("/+$", "") + path;
        Object raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Object.class);
        Map<?, ?> result = objectMapper.convertValue(raw, Map.class);
        Object status = result != null ? result.get("status") : null;
        if (!(status instanceof Number number) || number.intValue() != 200) {
            Object message = result != null ? result.get("message") : null;
            throw new IllegalStateException(message == null ? "主服务器无响应" : message.toString());
        }
        Object data = result.get("data");
        return data == null ? null : objectMapper.convertValue(data, dataType);
    }

    private boolean isConfigured() {
        return StringUtils.hasText(masterUrl) && StringUtils.hasText(token);
    }

    private boolean isNonRetryable(String msg) {
        if (!StringUtils.hasText(msg)) {
            return false;
        }
        return msg.contains("Cookie")
                || msg.contains("验证码")
                || msg.contains("安全验证")
                || msg.toLowerCase().contains("captcha");
    }

    private static String buildDefaultWorkerId() {
        String host = "unknown-host";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
