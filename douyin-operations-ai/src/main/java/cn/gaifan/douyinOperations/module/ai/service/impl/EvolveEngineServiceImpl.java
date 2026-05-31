package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.module.ai.gateway.AiCommercialFacade;
import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.event.AbTestWinnerEvent;
import cn.gaifan.douyinOperations.module.ai.event.EvolveTaskCompletedEvent;
import cn.gaifan.douyinOperations.module.ai.repository.*;
import cn.gaifan.douyinOperations.module.ai.util.EvolveModelOrderUtil;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionFitnessService;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionInfrastructureGate;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionReviewService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveCircuitBreakerService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveEngineService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveProgressStore;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTaskDagSupport;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库进化引擎服务实现（术语见 docs/modules/ai/EVOLUTION-TERMINOLOGY.md）。
 * <p>
 * 核心编排逻辑保留在本类；Prompt 构建委托 {@link EvolvePromptBuilder}，
 * 报告评分/持久化委托 {@link EvolveReportProcessor}，
 * 主题上下文收集/扩展委托 {@link EvolveTopicProcessor}，
 * 扩展 Agent 运行委托 {@link EvolveAgentRunner}。
 */
@Service
public class EvolveEngineServiceImpl implements EvolveEngineService {

    private static final Logger log = LoggerFactory.getLogger(EvolveEngineServiceImpl.class);
    private static final Set<String> TERMINAL_TASK_STATUSES = Set.of("completed", "failed", "canceled");
    private static final Set<String> DEFAULT_EVOLVE_KB_NAMES = Set.of("douyin", "zhishi", "huashu");
    private static final List<String> EXCLUDED_EVOLVE_KB_NAME_PARTS = List.of(
            "weigui", "违规", "violation", "risk", "redline", "红线"
    );

    @Value("${app.ai.evolve.quality-threshold:50}")
    private int qualityThreshold;
    @Value("${app.ai.evolve.deepen-threshold:25}")
    private int deepenThreshold;

    @Value("${app.ai.evolve.enabled:true}")
    private boolean evolveEnabled;
    @Value("${app.ai.evolve.kb-id:}")
    private Long evolveKbIdConfig;
    @Value("${app.ai.evolve.kb-name:}")
    private String evolveKbNameConfig;
    @Value("${app.ai.evolve.kb-ids:}")
    private String evolveKbIdsConfig;
    @Value("${app.ai.evolve.user-id:1}")
    private Long evolveUserId;
    @Value("${app.ai.evolve.multi-tenant:false}")
    private boolean evolveMultiTenant;
    @Value("${app.ai.evolution-fitness.auto-experiment-from-context:true}")
    private boolean autoExperimentFromContext;

    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Resource
    private AiEvolveTopicRepository topicRepository;
    @Resource
    private AiEvolveTaskRepository taskRepository;
    @Resource
    private AiEvolvePendingDeepenRepository pendingDeepenRepository;
    @Resource
    private AiEvolveReportRepository reportRepository;
    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;
    @Resource
    private LlmClient llmClient;
    @Resource
    private EvolveRoiService evolveRoiService;
    @Resource
    private EvolveProgressStore evolveProgressStore;
    @Resource
    private EvolveTopicService evolveTopicService;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;
    @Resource
    private TransactionTemplate transactionTemplate;

    // ---- delegate helpers ----
    @Resource
    private EvolvePromptBuilder promptBuilder;
    @Resource
    private EvolveReportProcessor reportProcessor;
    @Resource
    private EvolveTopicProcessor topicProcessor;
    @Resource
    private EvolveAgentRunner agentRunner;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private EvolutionReviewService evolutionReviewService;

    @Resource
    private EvolutionInfrastructureGate evolutionInfrastructureGate;

    @Resource
    private EvolutionFitnessService evolutionFitnessService;

    @Resource
    private EvolveCircuitBreakerService evolveCircuitBreakerService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AiCommercialFacade aiCommercialFacade;

    // ===================== runEvolution overloads =====================

    @Override
    @Async("evolveTaskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void runEvolution(Long kbId, String evolveAngleHint) {
        doRunEvolution(kbId, evolveAngleHint, null, null);
    }

    @Override
    @Async("evolveTaskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void runEvolution(Long kbId, String evolveAngleHint, String jobId) {
        doRunEvolution(kbId, evolveAngleHint, jobId, null);
    }

    @Override
    @Async("evolveTaskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void runEvolution(Long kbId, String evolveAngleHint, String jobId, List<String> dependsOnTaskNos) {
        doRunEvolution(kbId, evolveAngleHint, jobId, dependsOnTaskNos);
    }

    private void doRunEvolution(Long kbId, String evolveAngleHint, String jobId, List<String> dependsOnTaskNos) {
        if (!evolveEnabled) {
            log.info("进化引擎已禁用，跳过本轮进化");
            emitProgress(jobId, "skipped", "进化引擎已禁用", null);
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
            return;
        }
        String evolveTraceId = jobId != null && !jobId.isBlank()
                ? "evolve-" + jobId
                : Ids.compactUuid("trace_evolve");
        String evolveTenantId = "demo-tenant";
        if (!chargeEvolutionCommercial(evolveTenantId, evolveUserId, evolveTraceId)) {
            emitProgress(jobId, "skipped", "进化任务积分/授权不足", null);
            if (jobId != null && evolveProgressStore != null) {
                evolveProgressStore.complete(jobId);
            }
            return;
        }

        if (evolveCircuitBreakerService.isOpen()) {
            String reason = evolveCircuitBreakerService.currentReason();
            log.warn("进化引擎 LLM 熔断中，跳过本轮进化: {}", reason);
            emitProgress(jobId, "blocked", "LLM 熔断中: " + reason, null);
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
            return;
        }

        kbId = kbId != null ? kbId : resolveEvolveKbId();
        if (kbId == null) {
            log.warn("未找到可用的进化知识库，跳过本轮进化");
            emitProgress(jobId, "skipped", "未找到可用的进化知识库", null);
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
            return;
        }

        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId).orElse(null);
        long effectiveUserId = (kb != null && kb.getUserId() != null) ? kb.getUserId() : evolveUserId;

        List<AiModel> models = resolveEvolveModels();
        if (models == null || models.isEmpty()) {
            log.warn("未找到可用的进化模型，跳过本轮进化（kbId={}）", kbId);
            emitProgress(jobId, "skipped", "未找到可用的进化模型", null);
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
            return;
        }

        List<AiEvolveTopic> topics = evolveTopicService.sampleTopicsWithAttributionWeight(kbId, kb);
        if (topics.isEmpty()) {
            log.warn("主题池为空，跳过本轮进化（kbId={}），不会产生任务列表记录", kbId);
            emitProgress(jobId, "skipped", "主题池为空", null);
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
            return;
        }

        try {
            List<String> deps = normalizeDependsOnTaskNos(dependsOnTaskNos);
            // 秒级时间戳在「立即运行」多 Agent 并发时会重复，需加随机后缀保证 uk_task_no 唯一
            String taskNo = newEvolveTaskNo(kbId);

            if (!deps.isEmpty()) {
                for (String d : deps) {
                    AiEvolveTask depRow = taskRepository.findByTaskNo(d).orElseThrow(() ->
                            new BusinessException(ErrorCode.DATA_NOT_FOUND, "前置任务不存在: " + d));
                    if (!Objects.equals(depRow.getKbId(), kbId)) {
                        throw new BusinessException(ErrorCode.INVALID_PARAMS, "前置任务必须属于同一知识库: " + d);
                    }
                }
                List<AiEvolveTask> withDeps = taskRepository.findWithDependenciesByKbId(kbId);
                if (EvolveTaskDagSupport.wouldCreateCycle(withDeps, taskNo, deps)) {
                    throw new BusinessException(ErrorCode.INVALID_PARAMS, "进化任务依赖存在环，已拒绝创建");
                }
            }

            AiEvolveTask task = createTask(kbId, taskNo, topics, evolveAngleHint, deps);
            Map<String, String> statusByNo = loadStatusForTaskNos(deps);
            if (!deps.isEmpty() && !EvolveTaskDagSupport.allDependenciesCompleted(statusByNo, deps)) {
                List<String> inc = EvolveTaskDagSupport.incompleteDependencies(statusByNo, deps);
                task.setStatus("blocked");
                task.setBlockedReason(EvolveTaskDagSupport.blockedReasonForIncomplete(inc));
                taskRepository.save(task);
                emitProgress(jobId, "blocked", task.getBlockedReason(), task.getId());
                if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
                return;
            }

            task.setStatus("gathering");
            taskRepository.save(task);
            emitProgress(jobId, "gathering", "收集上下文中", task.getId());
            executeEvolutionCore(task, taskNo, kbId, kb, effectiveUserId, models, topics, evolveAngleHint, jobId);
        } catch (BusinessException ex) {
            log.warn("进化任务未启动: {}", ex.getMessage());
            emitProgress(jobId, "failed", ex.getMessage(), null);
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
        }
    }

    /**
     * 从 gathering 起执行主链路（新建任务或 blocked 解锁后复用）。
     */
    private void executeEvolutionCore(AiEvolveTask task, String taskNo, Long kbId, AiKnowledgeBase kb, long effectiveUserId,
                                      List<AiModel> models, List<AiEvolveTopic> topics, String evolveAngleHint, String jobId) {
        try {
            ensureTaskNotCanceled(task.getId());
            String context = topicProcessor.gatherContext(kbId, topics, effectiveUserId);
            ensureTaskNotCanceled(task.getId());
            task.setContextLength(context != null ? context.length() : 0);
            task.setStatus("generating");
            taskRepository.save(task);
            emitProgress(jobId, "generating", "LLM 生成中", task.getId());

            String evolveAngle = topicProcessor.pickEvolveAngle(kbId, evolveAngleHint);
            task.setEvolveAngle(evolveAngle);

            String qualityHint = "";
            String methodologyBlacklist = promptBuilder.buildMethodologyBlacklist(kbId);
            String lastRoundRef = promptBuilder.buildLastRoundRef(kbId);
            if (task.getHadQualityHint() != null && task.getHadQualityHint() == 1) {
                qualityHint = "\n【质量提醒】上轮报告得分偏低，请本轮特别注意方法论的可执行性和数据支撑。";
            }

            String kbType = kb != null && kb.getKbType() != null ? kb.getKbType().trim() : "general";
            boolean isHuashu = "huashu".equalsIgnoreCase(kbType);
            boolean isZhishi = "zhishi".equalsIgnoreCase(kbType);
            String prompt = isHuashu
                    ? promptBuilder.buildHuashuEvolvePrompt(context, evolveAngle, qualityHint, methodologyBlacklist, lastRoundRef)
                    : (isZhishi
                            ? promptBuilder.buildZhishiEvolvePrompt(context, evolveAngle, qualityHint, methodologyBlacklist, lastRoundRef)
                            : promptBuilder.buildEvolvePrompt(context, evolveAngle, qualityHint, methodologyBlacklist, lastRoundRef));
            long startMs = System.currentTimeMillis();
            String systemPrompt = isHuashu ? promptBuilder.getHuashuSystemPrompt() : (isZhishi ? promptBuilder.getZhishiSystemPrompt() : promptBuilder.getSystemPrompt());
            LlmClient.LlmResponse response = llmClient.chatWithFallback(models, systemPrompt, prompt);
            long durationMs = System.currentTimeMillis() - startMs;
            ensureTaskNotCanceled(task.getId());

            if (!response.success() || response.content() == null || response.content().length() < 200) {
                task.setStatus("failed");
                task.setErrorMessage("LLM 生成失败或内容过短: " + (response.errorMsg() != null ? response.errorMsg() : "无"));
                taskRepository.save(task);
                evolveCircuitBreakerService.recordFailure(task.getErrorMessage());
                emitProgress(jobId, "failed", task.getErrorMessage(), task.getId());
                if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
                return;
            }

            String reportContent = isHuashu ? reportProcessor.fixHuashuReportStructure(response.content()) : (isZhishi ? reportProcessor.fixZhishiReportStructure(response.content()) : reportProcessor.fixReportStructure(response.content()));
            if (isHuashu) {
                if ((!reportContent.contains("## 话术片段") && !reportContent.contains("话术")) || !reportContent.contains("##")) {
                    task.setStatus("failed");
                    task.setErrorMessage("话术进化报告缺少话术片段内容");
                    taskRepository.save(task);
                    emitProgress(jobId, "failed", task.getErrorMessage(), task.getId());
                    if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
                    return;
                }
            } else if (!isZhishi && !reportContent.contains("## 方法论提炼")) {
                task.setStatus("failed");
                task.setErrorMessage("报告缺少「方法论提炼」章节");
                taskRepository.save(task);
                emitProgress(jobId, "failed", task.getErrorMessage(), task.getId());
                if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
                return;
            }

            task.setModelUsed(response.success() ? models.get(0).getModelVersion() : "none");
            int tokensUsed = (int) Math.min(response.tokensUsed(), Integer.MAX_VALUE);
            task.setCompletionTokens(tokensUsed);
            task.setTotalTokens(tokensUsed);
            task.setDurationMs(durationMs);
            task.setStatus("scoring");
            taskRepository.save(task);
            emitProgress(jobId, "scoring", "质量评分中", task.getId());

            EvolveReportProcessor.QualityScoreResult scoreResult = isHuashu ? reportProcessor.scoreHuashuReport(reportContent) : (isZhishi ? reportProcessor.scoreZhishiReport(reportContent) : reportProcessor.scoreReport(reportContent));
            task.setScoreTotal(scoreResult.total());
            task.setScoreDetail(scoreResult.detailJson());
            taskRepository.save(task);

            AiEvolveReport report = reportProcessor.saveReport(task, reportContent, scoreResult);
            ensureTaskNotCanceled(task.getId());
            task.setStatus("expanding");
            taskRepository.save(task);
            emitProgress(jobId, "expanding", "主题扩展中", task.getId());

            reportProcessor.extractDeepenQuestions(report, task);

            int expandedCount = topicProcessor.expandTopics(report, task, kbId, kb, isHuashu, models);
            task.setExpandedCount(expandedCount);
            ensureTaskNotCanceled(task.getId());

            if (scoreResult.total() < deepenThreshold && !topics.isEmpty()) {
                for (AiEvolveTopic t : topics) {
                    t.setPriority(Math.max(0, t.getPriority() - 20));
                    topicRepository.save(t);
                }
                task.setDeepenedCount(topics.size());
            }

            boolean queuedForIndex = false;
            boolean awaitingHumanReview = false;
            if (scoreResult.total() >= qualityThreshold) {
                try {
                    evolutionInfrastructureGate.assertAutoMergeAllowed();
                    reportProcessor.pushToIndexQueue(report, kbId, isHuashu);
                    report.setIndexStatus("queued");
                    queuedForIndex = true;
                } catch (BusinessException ex) {
                    log.warn("进化高分报告未自动入索引队列（门闸或依赖不可用）: kbId={}, taskNo={}, {}",
                            kbId, taskNo, ex.getMessage());
                    report.setIndexStatus("skipped_gate");
                }
                reportRepository.save(report);
            } else if (scoreResult.total() >= 40 && evolutionReviewService != null) {
                // D-1: 灰色地带 [40, qualityThreshold) → 提交人工审核而非直接丢弃
                try {
                    String preview = reportContent.length() > 500 ? reportContent.substring(0, 500) : reportContent;
                    evolutionReviewService.createReviewTask(task.getId(), preview, scoreResult.total());
                    task.setStatus("pending_review");
                    taskRepository.save(task);
                    awaitingHumanReview = true;
                } catch (Exception ex) {
                    log.warn("创建审核任务失败: {}", ex.getMessage());
                }
            }

            topicRepository.incrementUsedCount(topics.stream().map(AiEvolveTopic::getId).collect(Collectors.toList()));
            if (!awaitingHumanReview) {
                task.setStatus("completed");
                task.setBlockedReason(null);
                taskRepository.save(task);
                emitProgress(jobId, "completed", "进化完成，得分 " + scoreResult.total(), task.getId());
            } else {
                task.setBlockedReason(null);
                taskRepository.save(task);
                emitProgress(jobId, "pending_review", "待人工审核（得分 " + scoreResult.total() + "）", task.getId());
            }
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);

            evolveRoiService.recordRun(1, queuedForIndex ? 1 : 0);
            log.info("进化任务 {} 完成，得分 {}，扩展主题 {} 个", taskNo, scoreResult.total(), expandedCount);

            String parentTaskNo = EvolveTaskDagSupport.parseDependsOnTaskNos(task.getDependsOnTaskNos()).stream()
                    .findFirst().orElse(null);
            Map<String, Object> fitnessPayload = new LinkedHashMap<>();
            fitnessPayload.put("qualityTotal", scoreResult.total());
            fitnessPayload.put("expandedCount", expandedCount);
            fitnessPayload.put("queuedForIndex", queuedForIndex);
            fitnessPayload.put("indexStatus", report.getIndexStatus());
            fitnessPayload.put("awaitingHumanReview", awaitingHumanReview);
            if (task.getAbExperimentId() != null && !task.getAbExperimentId().isBlank()) {
                fitnessPayload.put("abExperimentId", task.getAbExperimentId());
            }
            evolutionFitnessService.record(kbId, taskNo, parentTaskNo, "evolve_completed",
                    (double) scoreResult.total(), JSON.toJSONString(fitnessPayload), task.getAbExperimentId());

            // 待人工审核时任务保持 pending_review，不发布「已完成」事件，避免依赖 DAG 误判前置已完成
            if (!awaitingHumanReview) {
                applicationEventPublisher.publishEvent(new EvolveTaskCompletedEvent(kbId, task.getTaskNo()));
            }

        } catch (TaskCanceledException e) {
            log.info("进化任务 {} 已取消: {}", taskNo, e.getMessage());
            markTaskCanceled(task, e.getMessage(), jobId);
        } catch (Exception e) {
            log.error("进化任务 {} 执行失败", taskNo, e);
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
            taskRepository.save(task);
            evolveCircuitBreakerService.recordFailure(task.getErrorMessage());
            emitProgress(jobId, "failed", "失败: " + e.getMessage(), task.getId());
            if (jobId != null && evolveProgressStore != null) evolveProgressStore.complete(jobId);
        }
    }

    // ===================== blocked task resume =====================

    @Override
    public void tryResumeBlockedTasksAfterCompletion(Long kbId, String completedTaskNo) {
        if (kbId == null || completedTaskNo == null || completedTaskNo.isBlank()) return;
        List<AiEvolveTask> blocked = taskRepository.findByKbIdAndStatusOrderByCreateTimeAsc(kbId, "blocked");
        if (blocked.isEmpty()) return;
        Set<String> allRefs = new LinkedHashSet<>();
        allRefs.add(completedTaskNo);
        for (AiEvolveTask b : blocked) {
            allRefs.addAll(EvolveTaskDagSupport.parseDependsOnTaskNos(b.getDependsOnTaskNos()));
        }
        Map<String, String> statusByNo = loadStatusForTaskNos(new ArrayList<>(allRefs));
        for (AiEvolveTask b : blocked) {
            List<String> deps = EvolveTaskDagSupport.parseDependsOnTaskNos(b.getDependsOnTaskNos());
            if (deps.isEmpty() || !EvolveTaskDagSupport.allDependenciesCompleted(statusByNo, deps)) continue;
            Long bid = b.getId();
            try {
                transactionTemplate.executeWithoutResult(st -> resumeOneBlockedTask(bid));
            } catch (Exception ex) {
                log.warn("解锁进化任务失败 taskId={}", bid, ex);
            }
        }
    }

    protected void resumeOneBlockedTask(Long taskId) {
        AiEvolveTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || !"blocked".equals(task.getStatus())) return;
        Long kbId = task.getKbId();
        if (kbId == null) return;
        List<String> deps = EvolveTaskDagSupport.parseDependsOnTaskNos(task.getDependsOnTaskNos());
        Map<String, String> st = loadStatusForTaskNos(deps);
        if (!EvolveTaskDagSupport.allDependenciesCompleted(st, deps)) return;
        List<Long> tids = EvolveTaskDagSupport.parseTopicIdList(task.getTopicIds());
        if (tids.isEmpty()) {
            task.setStatus("failed");
            task.setErrorMessage("blocked 任务缺少有效 topicIds，无法恢复");
            taskRepository.save(task);
            return;
        }
        List<AiEvolveTopic> topics = topicRepository.findAllById(tids);
        if (topics.isEmpty()) {
            task.setStatus("failed");
            task.setErrorMessage("主题已失效，无法恢复进化任务");
            taskRepository.save(task);
            return;
        }
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId).orElse(null);
        long effectiveUserId = (kb != null && kb.getUserId() != null) ? kb.getUserId() : evolveUserId;
        List<AiModel> models = resolveEvolveModels();
        if (models == null || models.isEmpty()) {
            log.warn("解锁任务 {} 失败：无可用模型", task.getTaskNo());
            return;
        }
        String hint = task.getEvolveAngle() != null && !task.getEvolveAngle().isBlank() ? task.getEvolveAngle() : null;
        task.setBlockedReason(null);
        task.setStatus("gathering");
        taskRepository.save(task);
        executeEvolutionCore(task, task.getTaskNo(), kbId, kb, effectiveUserId, models, topics, hint, null);
    }

    // ===================== A/B test winner listener =====================

    /**
     * A/B 显著胜出（p < 0.05）→ 提取胜出策略关键词 → 创建高优先级进化主题
     */
    @EventListener
    public void onAbTestWinner(AbTestWinnerEvent event) {
        log.info("[Evolve] 收到 A/B 胜出事件: experiment={}, winner={}, improvement={}%",
                event.experimentKey(), event.winnerVariant(), String.format("%.1f", event.improvementPct()));
        try {
            Long kbId = resolveEvolveKbId();
            if (kbId == null) {
                log.debug("[Evolve] 无可用知识库，跳过 A/B 胜出主题创建");
                return;
            }

            // 创建高优先级主题：胜出策略
            AiEvolveTopic winnerTopic = new AiEvolveTopic();
            winnerTopic.setKbId(kbId);
            winnerTopic.setTopic("A/B胜出策略深化：" + event.experimentKey() + " - " + event.winnerVariant()
                    + "（提升" + String.format("%.1f", event.improvementPct()) + "%）");
            winnerTopic.setCategory("ab_test_winner");
            winnerTopic.setPriority(1); // 最高优先级
            winnerTopic.setSource("ab_test");
            topicRepository.save(winnerTopic);

            // 创建反模式主题：败出策略降权
            AiEvolveTopic antiTopic = new AiEvolveTopic();
            antiTopic.setKbId(kbId);
            antiTopic.setTopic("A/B反模式：" + event.experimentKey() + " - 避免非 "
                    + event.winnerVariant() + " 策略（已验证低效）");
            antiTopic.setCategory("anti_pattern");
            antiTopic.setPriority(50); // 较低优先级
            antiTopic.setSource("ab_test");
            topicRepository.save(antiTopic);

            log.info("[Evolve] A/B 胜出主题已创建: priority=1, experiment={}", event.experimentKey());
        } catch (Exception e) {
            log.warn("[Evolve] A/B 胜出主题创建失败: {}", e.getMessage());
        }
    }

    // ===================== CRUD delegates =====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiEvolveTopic saveTopic(AiEvolveTopic topic) {
        return evolveTopicService.saveTopic(topic);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTopic(Long id) {
        evolveTopicService.deleteTopic(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId) {
        pendingDeepenRepository.findByTaskId(taskId).forEach(pendingDeepenRepository::delete);
        reportRepository.findByTaskId(taskId).ifPresent(reportRepository::delete);
        taskRepository.findById(taskId).ifPresent(taskRepository::delete);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId) {
        AiEvolveTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "进化任务不存在"));
        String status = task.getStatus() != null ? task.getStatus().trim().toLowerCase(Locale.ROOT) : "";
        if ("canceled".equals(status)) {
            return;
        }
        if (TERMINAL_TASK_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "已结束任务不能取消");
        }
        task.setStatus("canceled");
        task.setBlockedReason(null);
        task.setErrorMessage("任务已被手动取消");
        taskRepository.save(task);
    }

    @Override
    public List<AiEvolveTopic> listTopics(Long kbId, Long accountId, boolean scopeGlobal) {
        return evolveTopicService.listTopics(kbId, accountId, scopeGlobal);
    }

    @Override
    public List<AiEvolveTask> listRecentTasks(int limit) {
        return taskRepository.findAll(
                PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createTime"))
        ).getContent();
    }

    @Override
    public List<AiEvolveTask> listScoredTasksForTrendSince(Timestamp since, Long kbId) {
        List<AiEvolveTask> tasks = taskRepository.findScoredTasksSince(since);
        return tasks.stream()
                .filter(t -> t.getScoreTotal() != null && t.getScoreTotal() > 0)
                .filter(t -> kbId == null || kbId.equals(t.getKbId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getScoreTrend(int days) {
        java.sql.Timestamp since = java.sql.Timestamp.valueOf(
                LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay());
        List<AiEvolveTask> tasks = taskRepository.findScoredTasksSince(since);
        Map<String, List<Integer>> byDate = new LinkedHashMap<>();
        for (AiEvolveTask t : tasks) {
            if (t.getCreateTime() == null || t.getScoreTotal() == null) continue;
            String date = t.getCreateTime().toLocalDateTime().toLocalDate().toString();
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(t.getScoreTotal());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> e : byDate.entrySet()) {
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
            result.add(Map.<String, Object>of("date", e.getKey(), "avgScore", Math.round(avg * 100) / 100.0, "count", e.getValue().size()));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopicDistribution() {
        return evolveTopicService.getTopicDistribution();
    }

    // ===================== agent delegates =====================

    @Override
    public String runCompetitorKnowledgeAgent(Long userId, String competitorInfo) {
        return agentRunner.runCompetitorKnowledgeAgent(userId, competitorInfo, resolveEvolveModels());
    }

    @Override
    public String runFeedbackDrivenAgent(Long userId) {
        return agentRunner.runFeedbackDrivenAgent(userId, resolveEvolveModels());
    }

    @Override
    public String runMultiModalIndexAgent(Long userId, String mediaUrl, String mediaType) {
        return agentRunner.runMultiModalIndexAgent(userId, mediaUrl, mediaType, resolveEvolveModels());
    }

    // ===================== KB / model resolution =====================

    @Override
    public List<Long> resolveEvolveKbIds() {
        if (evolveKbIdsConfig != null && !evolveKbIdsConfig.isBlank()) {
            return Arrays.stream(evolveKbIdsConfig.split("[,，\\s]+"))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; } })
                    .filter(Objects::nonNull).toList();
        }
        if (evolveKbIdConfig != null && evolveKbIdConfig > 0) {
            return List.of(evolveKbIdConfig);
        }
        if (evolveKbNameConfig != null && !evolveKbNameConfig.isBlank()) {
            Long resolved = knowledgeBaseService.resolveKbIdByName(evolveUserId, evolveKbNameConfig.trim());
            if (resolved != null) return List.of(resolved);
        }
        if (evolveMultiTenant) {
            List<Long> userIds = knowledgeBaseRepository.findDistinctUserIds();
            List<Long> allKbIds = new ArrayList<>();
            for (Long uid : userIds) {
                knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(uid, 0)
                        .stream()
                        .filter(this::isDefaultEvolveKb)
                        .map(AiKnowledgeBase::getId)
                        .forEach(allKbIds::add);
            }
            return allKbIds;
        }
        return knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(evolveUserId, 0)
                .stream()
                .filter(this::isDefaultEvolveKb)
                .map(AiKnowledgeBase::getId)
                .toList();
    }

    // ===================== private helpers =====================

    private Long resolveEvolveKbId() {
        if (evolveKbIdConfig != null && evolveKbIdConfig > 0) return evolveKbIdConfig;
        if (evolveKbNameConfig != null && !evolveKbNameConfig.isBlank()) {
            Long resolved = knowledgeBaseService.resolveKbIdByName(evolveUserId, evolveKbNameConfig.trim());
            if (resolved != null) return resolved;
        }
        var list = knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(evolveUserId, 0);
        return list.stream()
                .filter(this::isDefaultEvolveKb)
                .map(AiKnowledgeBase::getId)
                .findFirst()
                .orElse(null);
    }

    private boolean isDefaultEvolveKb(AiKnowledgeBase kb) {
        if (kb == null || kb.getId() == null || kb.getDeleted() != 0) {
            return false;
        }
        String name = kb.getKbName() == null ? "" : kb.getKbName().trim().toLowerCase(Locale.ROOT);
        if (name.isBlank()) {
            return false;
        }
        for (String excluded : EXCLUDED_EVOLVE_KB_NAME_PARTS) {
            if (name.contains(excluded.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return DEFAULT_EVOLVE_KB_NAMES.contains(name);
    }

    private List<AiModel> resolveEvolveModels() {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("knowledge_evolve", 1, 0);
        if (config.isPresent()) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long modelId : List.of(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (modelId != null) {
                    modelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                            .ifPresent(result::add);
                }
            }
            if (!result.isEmpty()) return result;
        }
        var all = modelRepository.findByStatusAndDeleted(1, 0);
        List<AiModel> chain = EvolveModelOrderUtil.buildOrderedChain(all);
        if (!chain.isEmpty()) {
            log.info("知识进化模型降级链（未配置 knowledge_evolve 任务模型）: {}",
                    chain.stream().map(m -> m.getModelProvider() + "/" + m.getModelVersion()).collect(Collectors.joining(" => ")));
        }
        return chain;
    }

    /** evolve_{kbId}_{yyyyMMdd_HHmmss}_{10位hex}，总长不超过 task_no 列 64 */
    private static String newEvolveTaskNo(long kbId) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return "evolve_" + kbId + "_" + ts + "_" + suffix;
    }

    private static List<String> normalizeDependsOnTaskNos(List<String> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }

    private Map<String, String> loadStatusForTaskNos(List<String> nos) {
        if (nos == null || nos.isEmpty()) return Map.of();
        List<AiEvolveTask> rows = taskRepository.findByTaskNoIn(nos);
        return rows.stream().collect(Collectors.toMap(AiEvolveTask::getTaskNo,
                t -> t.getStatus() != null ? t.getStatus() : "", (a, b) -> a));
    }

    private void emitProgress(String jobId, String step, String message, Long taskId) {
        if (jobId == null || evolveProgressStore == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("step", step);
        data.put("message", message);
        if (taskId != null) data.put("taskId", taskId);
        evolveProgressStore.emit(jobId, "progress", data);
    }

    private AiEvolveTask createTask(Long kbId, String taskNo, List<AiEvolveTopic> topics, String evolveAngleHint, List<String> dependsOnTaskNos) {
        AiEvolveTask task = new AiEvolveTask();
        task.setKbId(kbId);
        task.setTaskNo(taskNo);
        task.setTopicIds("[" + topics.stream().map(t -> String.valueOf(t.getId())).collect(Collectors.joining(",")) + "]");
        task.setTopicTexts("[" + topics.stream().map(t -> "\"" + t.getTopic().replace("\"", "\\\"") + "\"").collect(Collectors.joining(",")) + "]");
        if (evolveAngleHint != null && !evolveAngleHint.isBlank()) {
            task.setEvolveAngle(evolveAngleHint);
        }
        task.setDependsOnTaskNos(EvolveTaskDagSupport.serializeDependsOn(dependsOnTaskNos));
        String abId = resolveAbExperimentIdFromTopics(topics);
        if (abId != null) {
            task.setAbExperimentId(abId);
        }
        return taskRepository.save(task);
    }

    /**
     * 从主题池约定解析实验 ID：{@code category} 以 {@code ab:} 前缀开头时，余下为实验 id（最长 64）。
     */
    private String resolveAbExperimentIdFromTopics(List<AiEvolveTopic> topics) {
        if (!autoExperimentFromContext || topics == null || topics.isEmpty()) {
            return null;
        }
        for (AiEvolveTopic t : topics) {
            if (t == null) {
                continue;
            }
            String c = t.getCategory();
            if (c == null || !c.startsWith("ab:")) {
                continue;
            }
            String id = c.substring(3).trim();
            if (id.isEmpty()) {
                continue;
            }
            return id.length() > 64 ? id.substring(0, 64) : id;
        }
        return null;
    }

    private void ensureTaskNotCanceled(Long taskId) {
        if (taskId == null) {
            return;
        }
        AiEvolveTask latest = taskRepository.findById(taskId).orElse(null);
        if (latest != null && "canceled".equalsIgnoreCase(latest.getStatus())) {
            throw new TaskCanceledException(latest.getErrorMessage());
        }
    }

    private void markTaskCanceled(AiEvolveTask task, String message, String jobId) {
        task.setStatus("canceled");
        task.setBlockedReason(null);
        task.setErrorMessage(message != null && !message.isBlank() ? message : "任务已被手动取消");
        taskRepository.save(task);
        emitProgress(jobId, "canceled", task.getErrorMessage(), task.getId());
        if (jobId != null && evolveProgressStore != null) {
            evolveProgressStore.complete(jobId);
        }
    }

    /**
     * 进化任务商业化钩子：积分 consume + gf_ai_invocation 台账（不重写进化引擎）。
     */
    private boolean chargeEvolutionCommercial(String tenantId, Long userId, String traceId) {
        if (aiCommercialFacade == null) {
            return true;
        }
        String gfUser = userId != null ? "user-" + userId : "evolve-user";
        AiCommercialFacade.CommercialChargeResult result = aiCommercialFacade.chargeRound(
                FeatureCode.AI_EVOLUTION, "EVOLVE", traceId, tenantId, gfUser);
        if (!result.allowed()) {
            log.warn("进化任务扣费失败: {}", result.reason());
            return false;
        }
        return true;
    }

    private static final class TaskCanceledException extends RuntimeException {
        private TaskCanceledException(String message) {
            super(message != null && !message.isBlank() ? message : "任务已被手动取消");
        }
    }
}
