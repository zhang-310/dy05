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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int consume(int batchSize) {
        List<AiIndexQueue> pending = indexQueueRepository.findPendingTasks(PageRequest.of(0, batchSize));
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
                log.error("索引队列任务异常: queueTaskId={}, targetKbId={}, sourceType={}, sourceId={}",
                        task.getId(), task.getTargetKbId(), task.getSourceType(), task.getSourceId(), e);
                String err = e.getMessage() != null && e.getMessage().length() > 500 ? e.getMessage().substring(0, 500) : e.getMessage();
                if (task.getRetryCount() != null && task.getRetryCount() >= 2) {
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
