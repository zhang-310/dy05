package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Distributed account collection queue health snapshot.
 */
@Data
public class AccountCollectQueueHealthVO {

    private Long totalTasks;
    private Long pendingTasks;
    private Long duePendingTasks;
    private Long collectingTasks;
    private Long expiredCollectingTasks;
    private Long collectedTasks;
    private Long analyzingTasks;
    private Long indexingTasks;
    private Long completedTasks;
    private Long failedTasks;
    private Long retryableFailedTasks;
    private String healthStatus;
    private List<String> recommendations = new ArrayList<>();
    private List<WorkerHealthVO> workers = new ArrayList<>();
    private List<FailedTaskBriefVO> recentFailures = new ArrayList<>();

    @Data
    public static class WorkerHealthVO {
        private String workerId;
        private String workerRegion;
        private String status;
        private Boolean online;
        private Long currentTaskId;
        private Long collectingTasks;
        private Long expiredTasks;
        private Timestamp lastHeartbeatAt;
        private Timestamp lastSeenAt;
        private Timestamp leaseUntil;
        private Timestamp maxLeaseUntil;
        private Timestamp firstClaimedAt;
        private Timestamp lastClaimAt;
        private Timestamp lastSubmitAt;
        private Timestamp lastFailAt;
        private Integer successCount;
        private Integer failCount;
        private String lastError;
    }

    @Data
    public static class FailedTaskBriefVO {
        private Long id;
        private String accountName;
        private String inputType;
        private Integer retryCount;
        private Integer maxRetryCount;
        private String errorMessage;
        private Timestamp updateTime;
    }
}
