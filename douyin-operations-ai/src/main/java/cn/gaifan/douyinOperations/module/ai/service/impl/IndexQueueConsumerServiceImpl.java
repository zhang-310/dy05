package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveReport;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiIndexQueue;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionFitnessService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTaskDagSupport;
import cn.gaifan.douyinOperations.module.ai.service.IndexQueueConsumerService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 索引队列消费者：将进化报告/爆款拆解/直播复盘内容入库到知识库
 */
@Service
public class IndexQueueConsumerServiceImpl implements IndexQueueConsumerService {

    private static final Logger log = LoggerFactory.getLogger(IndexQueueConsumerServiceImpl.class);
    private static final Pattern TITLE_PATTERN = Pattern.compile("^#+\\s*(.+)$", Pattern.MULTILINE);

    @Resource
    private AiIndexQueueRepository indexQueueRepository;

    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private AiEvolveReportRepository evolveReportRepository;

    @Resource
    private AiEvolveTaskRepository evolveTaskRepository;

    @Resource
    private EvolutionFitnessService evolutionFitnessService;

    /** 通过代理调用 processOneTask，使 REQUIRES_NEW 生效；ObjectProvider 替代 @Lazy 自注入，避免 DevTools 下 CGLIB ClassCastException */
    @Resource
    private ObjectProvider<IndexQueueConsumerService> selfProvider;

    @Value("${app.ai.index-queue.consumer.max-retry-count:12}")
    private int maxRetryCount;

    @Value("${app.ai.index-queue.consumer.failed-backoff-minutes:10}")
    private int failedBackoffMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int consume(int batchSize) {
        List<AiIndexQueue> pending = findTasksToProcess(batchSize);
        if (pending.isEmpty()) return 0;

        int success = 0;
        for (AiIndexQueue task : pending) {
            int updated = indexQueueRepository.markProcessing(task.getId());
            if (updated == 0) continue;

            long processStartMs = System.currentTimeMillis();
            try {
                selfProvider.getObject().processOneTask(task);
                indexQueueRepository.markDone(task.getId());
                success++;
                log.info("索引队列消费成功: id={}, sourceType={}, targetKbId={}", task.getId(), task.getSourceType(), task.getTargetKbId());
                recordEvolvedIndexFitness(task, "index_succeeded", null, System.currentTimeMillis() - processStartMs);
            } catch (Exception e) {
                String err = truncateThrowableMessage(e, 500);
                if (shouldSkipNoIndexableChunks(task, e)) {
                    indexQueueRepository.markDone(task.getId());
                    success++;
                    String skipMsg = "skipped_no_indexable_chunks: " + (err != null ? err : "no indexable chunks");
                    log.info("索引队列任务无新增可索引分块，按已处理跳过: id={}, sourceType={}, targetKbId={}, error={}",
                            task.getId(), task.getSourceType(), task.getTargetKbId(), err);
                    recordEvolvedIndexFitness(task, "index_succeeded", skipMsg, System.currentTimeMillis() - processStartMs);
                    continue;
                }
                log.error("索引队列任务异常: queueTaskId={}, targetKbId={}, sourceType={}, sourceId={}",
                        task.getId(), task.getTargetKbId(), task.getSourceType(), task.getSourceId(), e);
                int currentRetry = task.getRetryCount() != null ? task.getRetryCount() : 0;
                if (currentRetry >= maxRetryCount - 1) {
                    indexQueueRepository.markFailed(task.getId(), err);
                    log.warn("索引队列任务失败(已达重试上限): id={}, error={}", task.getId(), err);
                    recordEvolvedIndexFitness(task, "index_failed", err, System.currentTimeMillis() - processStartMs);
                } else {
                    indexQueueRepository.markRetry(task.getId(), err);
                    log.warn("索引队列任务失败(将重试): id={}, retryCount={}, error={}", task.getId(), (task.getRetryCount() != null ? task.getRetryCount() : 0) + 1, err);
                }
            }
        }
        if (success > 0) {
            int cleaned = indexQueueRepository.cleanDone();
            log.debug("索引队列清理 done 记录 {} 条", cleaned);
        }
        return success;
    }

    private List<AiIndexQueue> findTasksToProcess(int batchSize) {
        int safeBatchSize = Math.max(1, batchSize);
        int cappedMaxRetry = Math.max(4, maxRetryCount);
        List<AiIndexQueue> tasks = new ArrayList<>(indexQueueRepository.findPendingTasks(
                cappedMaxRetry, PageRequest.of(0, safeBatchSize)));
        int remaining = safeBatchSize - tasks.size();
        if (remaining <= 0) {
            return tasks;
        }

        int backoff = Math.max(1, failedBackoffMinutes);
        List<AiIndexQueue> recoverable = indexQueueRepository.findRecoverableFailedTasks(
                cappedMaxRetry, backoff, PageRequest.of(0, remaining));
        for (AiIndexQueue task : recoverable) {
            String msg = task.getErrorMsg();
            if (msg != null && msg.length() > 420) {
                msg = msg.substring(0, 420);
            }
            int updated = indexQueueRepository.requeueFailed(task.getId(), "自动重试恢复: " + (msg != null ? msg : ""));
            if (updated > 0) {
                task.setStatus("pending");
                tasks.add(task);
                log.info("索引队列 failed 任务重新入队: id={}, retryCount={}, sourceType={}, targetKbId={}",
                        task.getId(), task.getRetryCount(), task.getSourceType(), task.getTargetKbId());
            }
        }
        return tasks;
    }

    private boolean shouldSkipNoIndexableChunks(AiIndexQueue task, Throwable e) {
        if (task == null) {
            return false;
        }
        String sourceType = task.getSourceType();
        if (!isAutomatedSourceType(sourceType)) {
            return false;
        }
        String msg = flattenThrowableMessage(e);
        if (msg.isBlank()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return msg.contains("无可索引分块")
                || msg.contains("chunk 级去重全部被跳过")
                || msg.contains("内容过短")
                || lower.contains("no indexable chunk");
    }

    private boolean isAutomatedSourceType(String sourceType) {
        return "evolved".equals(sourceType)
                || "evolved_script".equals(sourceType)
                || "viral_video".equals(sourceType)
                || "viral_analysis".equals(sourceType)
                || "live_review".equals(sourceType)
                || "live_script".equals(sourceType);
    }

    private static String truncateThrowableMessage(Throwable e, int maxLen) {
        String msg = flattenThrowableMessage(e);
        if (msg.isBlank()) {
            msg = e != null ? e.getClass().getSimpleName() : null;
        }
        if (msg == null || maxLen <= 0 || msg.length() <= maxLen) {
            return msg;
        }
        return msg.substring(0, maxLen);
    }

    private static String flattenThrowableMessage(Throwable e) {
        if (e == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable cur = e;
        while (cur != null) {
            if (sb.length() > 0) {
                sb.append(" | caused by: ");
            }
            String message = cur.getMessage();
            sb.append(message != null && !message.isBlank() ? message : cur.getClass().getSimpleName());
            cur = cur.getCause();
        }
        return sb.toString();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processOneTask(AiIndexQueue task) {
        processTask(task);
    }

    protected void processTask(AiIndexQueue task) {
        Long kbId = task.getTargetKbId();
        if (kbId == null) {
            throw new BusinessException(ErrorCode.AI_KNOWLEDGE_SEARCH_FAIL, "索引任务缺少 targetKbId");
        }

        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在: " + kbId));

        String content = task.getContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "索引任务内容为空");
        }

        String title = extractTitle(content, task.getSourceType(), task.getSourceId());
        knowledgeBaseService.uploadDocument(kbId, title, content, "md", kb.getUserId(), task.getSourceType());
    }

    /**
     * 进化报告入索引队列成功/最终失败时写入适应度（sourceType evolved / evolved_script）。
     */
    private void recordEvolvedIndexFitness(AiIndexQueue queueTask, String metricName, String errorMsg, long processMs) {
        if (evolutionFitnessService == null || queueTask == null || metricName == null) {
            return;
        }
        String st = queueTask.getSourceType();
        if (st == null || (!"evolved".equals(st) && !"evolved_script".equals(st))) {
            return;
        }
        Long reportId = queueTask.getSourceId();
        if (reportId == null) {
            return;
        }
        AiEvolveReport report = evolveReportRepository.findById(reportId).orElse(null);
        if (report == null || report.getTaskId() == null) {
            return;
        }
        AiEvolveTask evolveTask = evolveTaskRepository.findById(report.getTaskId()).orElse(null);
        if (evolveTask == null || evolveTask.getTaskNo() == null || evolveTask.getTaskNo().isBlank()) {
            return;
        }
        String parentNo = EvolveTaskDagSupport.parseDependsOnTaskNos(evolveTask.getDependsOnTaskNos()).stream()
                .findFirst().orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("queueId", queueTask.getId());
        payload.put("reportId", reportId);
        payload.put("sourceType", st);
        payload.put("processMs", processMs);
        if (errorMsg != null && !errorMsg.isBlank()) {
            payload.put("error", errorMsg);
        }
        if (evolveTask.getAbExperimentId() != null && !evolveTask.getAbExperimentId().isBlank()) {
            payload.put("abExperimentId", evolveTask.getAbExperimentId());
        }
        Double value = "index_succeeded".equals(metricName) ? 1.0 : 0.0;
        evolutionFitnessService.record(queueTask.getTargetKbId(), evolveTask.getTaskNo(), parentNo,
                metricName, value, JSON.toJSONString(payload), evolveTask.getAbExperimentId());
    }

    private String extractTitle(String content, String sourceType, Long sourceId) {
        Matcher m = TITLE_PATTERN.matcher(content);
        if (m.find()) {
            String t = m.group(1).trim();
            if (t.length() <= 128) return t;
        }
        String prefix = content.trim().split("\n")[0];
        if (prefix.length() > 128) prefix = prefix.substring(0, 125) + "...";
        return prefix.isEmpty() ? String.format("%s-%d", sourceType, sourceId) : prefix;
    }
}
