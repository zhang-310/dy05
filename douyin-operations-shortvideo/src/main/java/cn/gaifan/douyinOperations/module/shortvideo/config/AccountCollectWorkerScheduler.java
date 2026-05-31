package cn.gaifan.douyinOperations.module.shortvideo.config;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.AccountCollectAsyncRunner;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * DB-backed account collection worker.
 * Multiple servers can poll the same table safely through FOR UPDATE SKIP LOCKED.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.shortvideo.account-collect.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AccountCollectWorkerScheduler {

    private final SvAccountCollectTaskRepository taskRepository;
    private final AccountCollectAsyncRunner asyncRunner;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentMap<Long, Boolean> localRunning = new ConcurrentHashMap<>();

    @Value("${app.shortvideo.account-collect.worker.id:}")
    private String configuredWorkerId;

    @Value("${app.shortvideo.account-collect.worker.region:local}")
    private String workerRegion;

    @Value("${app.shortvideo.account-collect.worker.batch-size:1}")
    private int batchSize;

    @Value("${app.shortvideo.account-collect.worker.lease-seconds:900}")
    private int leaseSeconds;

    private String workerId;

    public AccountCollectWorkerScheduler(SvAccountCollectTaskRepository taskRepository,
                                         AccountCollectAsyncRunner asyncRunner,
                                         TransactionTemplate transactionTemplate) {
        this.taskRepository = taskRepository;
        this.asyncRunner = asyncRunner;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    public void init() {
        workerId = StringUtils.hasText(configuredWorkerId) ? configuredWorkerId.trim() : buildDefaultWorkerId();
        log.info("账号采集 worker 已启动: workerId={}, region={}, batchSize={}, leaseSeconds={}",
                workerId, workerRegion, batchSize, leaseSeconds);
    }

    @Scheduled(fixedDelayString = "${app.shortvideo.account-collect.worker.poll-ms:15000}",
            initialDelayString = "${app.shortvideo.account-collect.worker.initial-delay-ms:10000}")
    public void poll() {
        int limit = Math.max(1, batchSize);
        List<Long> claimed = transactionTemplate.execute(status -> claimTasks(limit));
        if (claimed == null || claimed.isEmpty()) {
            return;
        }
        for (Long taskId : claimed) {
            if (localRunning.putIfAbsent(taskId, Boolean.TRUE) != null) {
                continue;
            }
            SvAccountCollectTask task = taskRepository.findById(taskId).orElse(null);
            if (task == null) {
                localRunning.remove(taskId);
                continue;
            }
            asyncRunner.runCollectAsync(taskId, task.getOwnerId(), () -> localRunning.remove(taskId));
            log.info("账号采集任务已由 worker 领取: taskId={}, ownerId={}, workerId={}",
                    taskId, task.getOwnerId(), workerId);
        }
    }

    @Scheduled(fixedDelayString = "${app.shortvideo.account-collect.worker.heartbeat-ms:60000}",
            initialDelayString = "${app.shortvideo.account-collect.worker.heartbeat-initial-delay-ms:30000}")
    public void heartbeat() {
        Timestamp leaseUntil = new Timestamp(System.currentTimeMillis() + Math.max(60, leaseSeconds) * 1000L);
        for (Long taskId : localRunning.keySet()) {
            int updated = taskRepository.heartbeat(taskId, workerId, leaseUntil);
            if (updated == 0) {
                localRunning.remove(taskId);
                log.warn("账号采集 worker 心跳失效，停止本地跟踪: taskId={}, workerId={}", taskId, workerId);
            }
        }
    }

    private List<Long> claimTasks(int limit) {
        List<Long> pending = taskRepository.findClaimablePendingTaskIds(limit);
        List<Long> expired = pending.size() >= limit
                ? List.of()
                : taskRepository.findExpiredCollectingTaskIds(limit - pending.size());

        Set<Long> candidates = new LinkedHashSet<>();
        candidates.addAll(pending);
        candidates.addAll(expired);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Timestamp leaseUntil = new Timestamp(System.currentTimeMillis() + Math.max(60, leaseSeconds) * 1000L);
        List<Long> claimed = new ArrayList<>();
        for (Long id : candidates) {
            if (localRunning.containsKey(id)) {
                continue;
            }
            int updated = taskRepository.claimForCollect(id, workerId, workerRegion, leaseUntil);
            if (updated > 0) {
                claimed.add(id);
            }
        }
        return claimed;
    }

    private static String buildDefaultWorkerId() {
        String host = "unknown-host";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            // Fall back to UUID below.
        }
        return host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
