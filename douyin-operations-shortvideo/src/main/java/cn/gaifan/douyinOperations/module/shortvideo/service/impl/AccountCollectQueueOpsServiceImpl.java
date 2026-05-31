package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectWorkerNode;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectWorkerNodeRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.AccountCollectQueueOpsService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectQueueHealthVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectQueueRepairVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountCollectQueueOpsServiceImpl implements AccountCollectQueueOpsService {

    @Resource
    private SvAccountCollectTaskRepository taskRepository;
    @Resource
    private SvAccountCollectWorkerNodeRepository workerNodeRepository;

    @Override
    public AccountCollectQueueHealthVO health(Long userId) {
        AccountCollectQueueHealthVO vo = new AccountCollectQueueHealthVO();
        vo.setTotalTasks(taskRepository.countByOwnerIdAndDeleted(userId, 0));
        vo.setPendingTasks(count(userId, "pending"));
        vo.setDuePendingTasks(taskRepository.countDuePending(userId));
        vo.setCollectingTasks(count(userId, "collecting"));
        vo.setExpiredCollectingTasks(taskRepository.countExpiredCollecting(userId));
        vo.setCollectedTasks(count(userId, "collected"));
        vo.setAnalyzingTasks(count(userId, "analyzing"));
        vo.setIndexingTasks(count(userId, "indexing"));
        vo.setCompletedTasks(count(userId, "completed"));
        vo.setFailedTasks(count(userId, "failed"));
        vo.setRetryableFailedTasks(taskRepository.countRetryableFailed(userId));
        vo.setWorkers(loadWorkers(userId));
        vo.setRecentFailures(loadRecentFailures(userId));
        vo.setRecommendations(buildRecommendations(vo));
        vo.setHealthStatus(resolveHealthStatus(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountCollectQueueRepairVO repair(Long userId) {
        AccountCollectQueueRepairVO vo = new AccountCollectQueueRepairVO();
        vo.setReleasedExpired(taskRepository.releaseExpiredCollectingForRetry(
                userId, "租约已过期，系统自动释放并重新排队"));
        vo.setTerminalExpired(taskRepository.markExpiredCollectingTerminal(
                userId, "租约已过期且达到最大重试次数，系统标记为失败"));
        vo.setRetriedFailed(taskRepository.retryFailedTasks(
                userId, "系统批量重试失败采集任务"));
        return vo;
    }

    private long count(Long userId, String status) {
        return taskRepository.countByOwnerIdAndStatusAndDeleted(userId, status, 0);
    }

    private List<AccountCollectQueueHealthVO.WorkerHealthVO> loadWorkers(Long userId) {
        List<AccountCollectQueueHealthVO.WorkerHealthVO> workers = new ArrayList<>();
        for (Object[] row : taskRepository.aggregateWorkerHealth(userId)) {
            AccountCollectQueueHealthVO.WorkerHealthVO vo = new AccountCollectQueueHealthVO.WorkerHealthVO();
            vo.setWorkerId(asString(row[0]));
            vo.setWorkerRegion(asString(row[1]));
            vo.setStatus("collecting");
            vo.setOnline(true);
            vo.setCollectingTasks(asLong(row[2]));
            vo.setExpiredTasks(asLong(row[3]));
            vo.setLastHeartbeatAt(asTimestamp(row[4]));
            vo.setLastSeenAt(vo.getLastHeartbeatAt());
            vo.setMaxLeaseUntil(asTimestamp(row[5]));
            vo.setFirstClaimedAt(asTimestamp(row[6]));
            workers.add(vo);
        }
        for (SvAccountCollectWorkerNode node : workerNodeRepository.findByDeletedOrderByLastSeenAtDesc(0)) {
            AccountCollectQueueHealthVO.WorkerHealthVO vo = findWorker(workers, node.getWorkerId());
            if (vo == null) {
                vo = new AccountCollectQueueHealthVO.WorkerHealthVO();
                vo.setWorkerId(node.getWorkerId());
                vo.setWorkerRegion(node.getWorkerRegion());
                vo.setCollectingTasks(0L);
                vo.setExpiredTasks(0L);
                workers.add(vo);
            }
            vo.setStatus(resolveNodeStatus(node));
            vo.setOnline(isOnline(node.getLastSeenAt()));
            vo.setCurrentTaskId(node.getCurrentTaskId());
            vo.setLeaseUntil(node.getLeaseUntil());
            vo.setLastSeenAt(node.getLastSeenAt());
            vo.setLastClaimAt(node.getLastClaimAt());
            vo.setLastSubmitAt(node.getLastSubmitAt());
            vo.setLastFailAt(node.getLastFailAt());
            vo.setSuccessCount(node.getSuccessCount());
            vo.setFailCount(node.getFailCount());
            vo.setLastError(node.getLastError());
            if (vo.getWorkerRegion() == null) {
                vo.setWorkerRegion(node.getWorkerRegion());
            }
        }
        return workers;
    }

    private AccountCollectQueueHealthVO.WorkerHealthVO findWorker(List<AccountCollectQueueHealthVO.WorkerHealthVO> workers,
                                                                  String workerId) {
        if (workerId == null) {
            return null;
        }
        for (AccountCollectQueueHealthVO.WorkerHealthVO worker : workers) {
            if (workerId.equals(worker.getWorkerId())) {
                return worker;
            }
        }
        return null;
    }

    private String resolveNodeStatus(SvAccountCollectWorkerNode node) {
        if (!isOnline(node.getLastSeenAt())) {
            return "offline";
        }
        if (node.getCurrentTaskId() != null || "collecting".equals(node.getStatus())) {
            return "collecting";
        }
        return "idle";
    }

    private boolean isOnline(Timestamp lastSeenAt) {
        if (lastSeenAt == null) {
            return false;
        }
        return System.currentTimeMillis() - lastSeenAt.getTime() <= 5 * 60 * 1000L;
    }

    private List<AccountCollectQueueHealthVO.FailedTaskBriefVO> loadRecentFailures(Long userId) {
        List<AccountCollectQueueHealthVO.FailedTaskBriefVO> failures = new ArrayList<>();
        for (Object[] row : taskRepository.findRecentFailedRows(userId, 10)) {
            AccountCollectQueueHealthVO.FailedTaskBriefVO vo = new AccountCollectQueueHealthVO.FailedTaskBriefVO();
            vo.setId(asLong(row[0]));
            vo.setAccountName(asString(row[1]));
            vo.setInputType(asString(row[2]));
            vo.setRetryCount(asInt(row[3]));
            vo.setMaxRetryCount(asInt(row[4]));
            vo.setErrorMessage(asString(row[5]));
            vo.setUpdateTime(asTimestamp(row[6]));
            failures.add(vo);
        }
        return failures;
    }

    private List<String> buildRecommendations(AccountCollectQueueHealthVO vo) {
        List<String> tips = new ArrayList<>();
        if (safe(vo.getExpiredCollectingTasks()) > 0) {
            tips.add("存在过期租约任务，建议执行队列修复释放给其他 worker 接力。");
        }
        if (safe(vo.getRetryableFailedTasks()) > 0) {
            tips.add("存在可重试失败任务，建议批量重试后观察 Cookie/风控/Playwright 日志。");
        }
        if (safe(vo.getFailedTasks()) > 0 && safe(vo.getRetryableFailedTasks()) == 0) {
            tips.add("存在终止失败任务，多为 Cookie、验证码或安全验证问题；请先更新采集账号环境后重新创建任务。");
        }
        if (safe(vo.getDuePendingTasks()) > 0 && safe(vo.getCollectingTasks()) == 0) {
            tips.add("有到期待采集任务但没有 worker 运行，请检查采集服务器配置 app.shortvideo.account-collect.worker.enabled。");
        }
        if (safe(vo.getWorkers()).isEmpty() && safe(vo.getPendingTasks()) > 0) {
            tips.add("当前没有活跃 worker 领取任务，多服务器部署时每台机器需要唯一 worker.id 和有效抖音 Cookie。");
        }
        if (!safe(vo.getWorkers()).isEmpty()
                && safe(vo.getWorkers()).stream().noneMatch(w -> Boolean.TRUE.equals(w.getOnline()))) {
            tips.add("采集节点表中已有 worker，但最近 5 分钟没有心跳；请检查 collector 容器、主服务器地址和 Token。");
        }
        if (tips.isEmpty()) {
            tips.add("采集队列没有发现阻塞项，可继续扩容 worker 或增加账号采集任务。");
        }
        return tips;
    }

    private String resolveHealthStatus(AccountCollectQueueHealthVO vo) {
        if (safe(vo.getExpiredCollectingTasks()) > 0) {
            return "degraded";
        }
        if (safe(vo.getFailedTasks()) > 0 && safe(vo.getRetryableFailedTasks()) == 0) {
            return "attention";
        }
        if (safe(vo.getDuePendingTasks()) > 0 && safe(vo.getCollectingTasks()) == 0) {
            return "blocked";
        }
        return "ok";
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static Integer asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Timestamp asTimestamp(Object value) {
        return value instanceof Timestamp timestamp ? timestamp : null;
    }

    private static long safe(Long value) {
        return value == null ? 0L : value;
    }

    private static <T> List<T> safe(List<T> value) {
        return value == null ? List.of() : value;
    }
}
