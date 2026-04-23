package cn.gaifan.douyinOperations.module.agent.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.entity.Agent;
import cn.gaifan.douyinOperations.module.agent.entity.AgentWorkflow;
import cn.gaifan.douyinOperations.module.agent.entity.AgentWorkflowExecution;
import cn.gaifan.douyinOperations.module.agent.entity.AgentWorkflowStep;
import cn.gaifan.douyinOperations.module.agent.repository.AgentRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentWorkflowExecutionRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentWorkflowRepository;
import cn.gaifan.douyinOperations.module.agent.repository.AgentWorkflowStepRepository;
import cn.gaifan.douyinOperations.module.agent.service.AgentWorkflowService;
import cn.gaifan.douyinOperations.module.agent.service.SkillExecutor;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowExecutionVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowStepVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

@Service
public class AgentWorkflowServiceImpl implements AgentWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowServiceImpl.class);
    private static final ObjectMapper OM = new ObjectMapper();

    @Resource
    private AgentWorkflowRepository workflowRepository;

    @Resource
    private AgentWorkflowStepRepository stepRepository;

    @Resource
    private AgentWorkflowExecutionRepository executionRepository;

    @Resource
    private AgentRepository agentRepository;

    @Resource
    private SkillExecutor skillExecutor;

    // 线程池：用于并行执行 DAG 同层步骤
    private final ExecutorService dagExecutor = Executors.newCachedThreadPool();

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    @Override
    public PageResultVO<AgentWorkflowVO> list(Long userId, int page, int rows) {
        if (page < 0) page = 0;
        if (rows < 1) rows = 10;
        if (rows > 50) rows = 50;

        Page<AgentWorkflow> p = workflowRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(
                userId, 0, PageRequest.of(page, rows));

        List<AgentWorkflowVO> vos = p.getContent().stream().map(this::workflowToVO).toList();
        return PageResultVO.of(p.getTotalElements(), vos, page + 1, rows);
    }

    @Override
    public AgentWorkflowVO getById(Long workflowId) {
        AgentWorkflow w = workflowRepository.findByIdAndDeleted(workflowId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流不存在"));
        return workflowToVO(w);
    }

    @Override
    @Transactional
    public long save(Long userId, AgentWorkflowSaveVO vo) {
        AgentWorkflow workflow;
        boolean isNew = vo.getId() == null || vo.getId() == 0;

        if (isNew) {
            workflow = new AgentWorkflow();
            workflow.setUserId(userId);
        } else {
            workflow = workflowRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流不存在"));
            stepRepository.findByWorkflowIdAndDeletedOrderByStepOrderAsc(workflow.getId(), 0)
                    .forEach(s -> { s.setDeleted(1); stepRepository.save(s); });
        }

        workflow.setName(vo.getName());
        workflow.setDescription(vo.getDescription());
        workflow.setStatus(vo.getStatus() != null ? vo.getStatus() : 1);
        workflow.setVersion(workflow.getVersion() + 1);
        AgentWorkflow saved = workflowRepository.save(workflow);

        for (AgentWorkflowSaveVO.StepVO stepVO : vo.getSteps()) {
            AgentWorkflowStep step = new AgentWorkflowStep();
            step.setWorkflowId(saved.getId());
            step.setStepOrder(stepVO.getStepOrder());
            step.setAgentId(stepVO.getAgentId());
            step.setStepName(stepVO.getStepName());
            step.setInputTemplate(stepVO.getInputTemplate());
            step.setOutputKey(stepVO.getOutputKey() != null ? stepVO.getOutputKey() : "step" + stepVO.getStepOrder());
            step.setSkipCondition(stepVO.getSkipCondition());
            step.setTimeoutSeconds(stepVO.getTimeoutSeconds() != null ? stepVO.getTimeoutSeconds() : 120);
            step.setExecutionMode(stepVO.getExecutionMode() != null ? stepVO.getExecutionMode() : 0);
            step.setRetryCount(stepVO.getRetryCount() != null ? stepVO.getRetryCount() : 1);
            if (stepVO.getDependsOn() != null && !stepVO.getDependsOn().isEmpty()) {
                try {
                    step.setDependsOn(OM.writeValueAsString(stepVO.getDependsOn()));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize dependsOn: {}", stepVO.getDependsOn());
                }
            }
            stepRepository.save(step);
        }

        return saved.getId();
    }

    @Override
    @Transactional
    public void delete(Long workflowId) {
        AgentWorkflow w = workflowRepository.findByIdAndDeleted(workflowId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流不存在"));
        w.setDeleted(1);
        workflowRepository.save(w);

        stepRepository.findByWorkflowIdAndDeletedOrderByStepOrderAsc(workflowId, 0)
                .forEach(s -> { s.setDeleted(1); stepRepository.save(s); });
    }

    // ─────────────────────────────────────────────
    // 执行历史查询
    // ─────────────────────────────────────────────

    @Override
    public PageResultVO<AgentWorkflowExecutionVO> getExecutionHistory(Long userId, int page, int rows) {
        if (page < 0) page = 0;
        if (rows < 1) rows = 10;
        if (rows > 50) rows = 50;

        Page<AgentWorkflowExecution> p = executionRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(
                userId, 0, PageRequest.of(page, rows));

        List<AgentWorkflowExecutionVO> vos = p.getContent().stream().map(this::execToVO).toList();
        return PageResultVO.of(p.getTotalElements(), vos, page + 1, rows);
    }

    @Override
    public PageResultVO<AgentWorkflowExecutionVO> getWorkflowExecutions(Long userId, Long workflowId, int page, int rows) {
        if (page < 0) page = 0;
        if (rows < 1) rows = 10;
        if (rows > 50) rows = 50;

        Page<AgentWorkflowExecution> p = executionRepository.findByWorkflowIdAndDeletedOrderByCreateTimeDesc(
                workflowId, 0, PageRequest.of(page, rows));

        List<AgentWorkflowExecutionVO> vos = p.getContent().stream().map(this::execToVO).toList();
        return PageResultVO.of(p.getTotalElements(), vos, page + 1, rows);
    }

    @Override
    public AgentWorkflowExecutionVO getExecutionById(Long executionId) {
        AgentWorkflowExecution e = executionRepository.findByIdAndDeleted(executionId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "执行记录不存在"));
        return execToVO(e);
    }

    // ─────────────────────────────────────────────
    // 核心执行：支持 DAG 并行
    // ─────────────────────────────────────────────

    @Override
    public Map<String, Object> execute(Long workflowId, Long userId, Long conversationId, String userInput) {
        AgentWorkflow workflow = workflowRepository.findByIdAndDeleted(workflowId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流不存在"));

        if (workflow.getStatus() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "工作流已禁用");
        }

        List<AgentWorkflowStep> steps = stepRepository.findByWorkflowIdAndDeletedOrderByStepOrderAsc(workflowId, 0);
        if (steps.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "工作流没有步骤");
        }

        // 持久化执行记录
        AgentWorkflowExecution execution = new AgentWorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setUserId(userId);
        execution.setStatus(AgentWorkflowExecution.STATUS_RUNNING);
        execution.setTotalSteps(steps.size());
        execution.setStartTime(new Timestamp(System.currentTimeMillis()));
        execution = executionRepository.save(execution);

        // 上下文 Map
        Map<String, String> context = new LinkedHashMap<>();
        context.put("input", userInput);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workflowId", workflowId);
        result.put("workflowName", workflow.getName());
        result.put("executionId", execution.getId());
        result.put("totalSteps", steps.size());
        result.put("userInput", userInput);

        List<Map<String, Object>> stepResults = new ArrayList<>();

        try {
            // 构建 DAG 层
            List<List<AgentWorkflowStep>> layers = buildDAGLayers(steps);
            result.put("dagLayers", layers.size());

            log.info("[Workflow] DAG layers={}, steps={}", layers.size(), steps.size());

            // 按层执行
            for (int layerIdx = 0; layerIdx < layers.size(); layerIdx++) {
                List<AgentWorkflowStep> layer = layers.get(layerIdx);
                log.info("[Workflow] Executing layer {} with {} steps", layerIdx, layer.size());

                List<Map<String, Object>> layerResults = executeLayer(layer, userId, conversationId, userInput, context);

                // 将层结果按步骤展开
                for (Map<String, Object> layerResult : layerResults) {
                    stepResults.add(layerResult);
                }

                // 持久化上下文
                persistContext(execution, context);

                // 任何层出错则中断（容错模式：记录错误但继续；严格模式可提前退出）
            }

            execution.setStatus(AgentWorkflowExecution.STATUS_COMPLETED);
            execution.setEndTime(new Timestamp(System.currentTimeMillis()));
            long completedCount = stepResults.stream()
                    .filter(s -> !(Boolean.TRUE.equals(s.get("skipped"))) && Boolean.TRUE.equals(s.get("success")))
                    .count();
            execution.setCurrentStepOrder((int) completedCount);
            executionRepository.save(execution);

            result.put("steps", stepResults);
            result.put("completedSteps", completedCount);
            result.put("finalOutput", context.get("finalOutput"));
            result.put("status", "completed");

        } catch (Exception e) {
            log.error("[Workflow] Execution failed: {}", e.getMessage(), e);
            execution.setStatus(AgentWorkflowExecution.STATUS_FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setEndTime(new Timestamp(System.currentTimeMillis()));
            executionRepository.save(execution);

            result.put("steps", stepResults);
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * 执行同一 DAG 层的所有步骤（可并行）
     */
    private List<Map<String, Object>> executeLayer(List<AgentWorkflowStep> layer, Long userId,
            Long conversationId, String userInput, Map<String, String> context) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (layer.size() == 1) {
            // 单步骤：直接顺序执行
            String lastOutput = context.get("lastOutput");
            Map<String, Object> r = executeStepWithRetry(layer.get(0), userId, conversationId, userInput, context, lastOutput);
            results.add(r);
        } else {
            // 多步骤：并行执行，同步等待所有完成
            CountDownLatch latch = new CountDownLatch(layer.size());
            ConcurrentHashMap<String, Map<String, Object>> concurrentResults = new ConcurrentHashMap<>();

            for (AgentWorkflowStep step : layer) {
                dagExecutor.submit(() -> {
                    try {
                        String lastOutput = context.get("lastOutput");
                        Map<String, Object> r = executeStepWithRetry(step, userId, conversationId,
                                userInput, context, lastOutput);
                        concurrentResults.put(getStepKey(step), r);
                    } catch (Exception e) {
                        log.error("[Workflow] Parallel step {} failed: {}", step.getStepOrder(), e.getMessage());
                        Map<String, Object> err = new LinkedHashMap<>();
                        err.put("stepOrder", step.getStepOrder());
                        err.put("stepName", step.getStepName());
                        err.put("success", false);
                        err.put("error", e.getMessage());
                        concurrentResults.put(getStepKey(step), err);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 按 stepOrder 顺序返回结果（保持确定性）
            layer.stream()
                    .sorted(Comparator.comparingInt(AgentWorkflowStep::getStepOrder))
                    .forEach(step -> results.add(concurrentResults.get(getStepKey(step))));
        }

        return results;
    }

    /**
     * 执行单个步骤（带重试）
     */
    private Map<String, Object> executeStepWithRetry(AgentWorkflowStep step, Long userId,
            Long conversationId, String userInput, Map<String, String> context, String lastOutput) {

        int retryCount = step.getRetryCount() != null ? step.getRetryCount() : 1;
        Exception lastException = null;

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                return executeSingleStep(step, userId, conversationId, userInput, context, lastOutput);
            } catch (Exception e) {
                lastException = e;
                if (attempt < retryCount) {
                    log.warn("[Workflow] Step {} attempt {} failed, retrying: {}",
                            step.getStepOrder(), attempt, e.getMessage());
                }
            }
        }

        // 所有重试均失败
        Map<String, Object> errorResult = new LinkedHashMap<>();
        errorResult.put("stepOrder", step.getStepOrder());
        errorResult.put("stepName", step.getStepName());
        errorResult.put("agentId", step.getAgentId());
        errorResult.put("success", false);
        errorResult.put("error", lastException != null ? lastException.getMessage() : "unknown");
        return errorResult;
    }

    /**
     * 执行单个步骤（不含重试）
     */
    private Map<String, Object> executeSingleStep(AgentWorkflowStep step, Long userId,
            Long conversationId, String userInput, Map<String, String> context, String lastOutput) {

        // 跳过条件检查
        if (shouldSkip(step.getSkipCondition(), userInput, lastOutput)) {
            log.info("[Workflow] Step {} skipped: {}", step.getStepOrder(), step.getSkipCondition());
            Map<String, Object> skipInfo = new LinkedHashMap<>();
            skipInfo.put("stepOrder", step.getStepOrder());
            skipInfo.put("stepName", step.getStepName());
            skipInfo.put("skipped", true);
            skipInfo.put("reason", "跳过条件满足");
            return skipInfo;
        }

        // 构建输入（替换占位符）
        String stepInput = buildStepInput(step.getInputTemplate(), userInput, lastOutput, context, step.getStepOrder());

        int timeout = step.getTimeoutSeconds() != null ? step.getTimeoutSeconds() : 120;
        log.info("[Workflow] Step {} executing: agentId={}, timeout={}s, input={}",
                step.getStepOrder(), step.getAgentId(), timeout,
                stepInput.length() > 100 ? stepInput.substring(0, 100) + "..." : stepInput);

        List<SkillExecutor.ToolCallResult> toolResults;
        try {
            Future<List<SkillExecutor.ToolCallResult>> future =
                    Executors.newSingleThreadExecutor().submit(() ->
                            skillExecutor.detectAndExecute(step.getAgentId(), userId, conversationId, stepInput));

            toolResults = future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "步骤执行超时（" + timeout + "秒）: " + step.getStepName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "步骤执行被中断: " + step.getStepName());
        } catch (ExecutionException e) {
            throw e.getCause() instanceof BusinessException
                    ? (BusinessException) e.getCause()
                    : new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "步骤执行失败: " + e.getCause().getMessage());
        }

        String output = skillExecutor.formatToolCallResults(toolResults);
        if (output == null || output.isBlank()) {
            output = "（此步骤未产生工具调用，由智能体直接响应）";
        }

        // 写入上下文
        String outputKey = step.getOutputKey() != null ? step.getOutputKey() : "step" + step.getStepOrder();
        context.put(outputKey, output);
        context.put("lastOutput", output);
        context.put("finalOutput", output);

        Map<String, Object> stepResult = new LinkedHashMap<>();
        stepResult.put("stepOrder", step.getStepOrder());
        stepResult.put("stepName", step.getStepName());
        stepResult.put("agentId", step.getAgentId());
        stepResult.put("outputKey", outputKey);
        stepResult.put("output", output);
        stepResult.put("toolCallCount", toolResults.size());
        stepResult.put("success", true);

        log.info("[Workflow] Step {} completed: toolCalls={}, outputLen={}",
                step.getStepOrder(), toolResults.size(), output.length());

        return stepResult;
    }

    // ─────────────────────────────────────────────
    // DAG 拓扑排序（Kahn 算法）
    // ─────────────────────────────────────────────

    /**
     * 根据 depends_on 构建 DAG，返回拓扑排序后的层级列表。
     * 每层内的步骤无依赖关系，可以并行执行。
     */
    private List<List<AgentWorkflowStep>> buildDAGLayers(List<AgentWorkflowStep> steps) {
        if (steps.isEmpty()) return Collections.emptyList();

        // 排序（保证 stepOrder 顺序稳定）
        steps.sort(Comparator.comparingInt(AgentWorkflowStep::getStepOrder));

        // 构建 outputKey → step 映射
        Map<String, AgentWorkflowStep> keyToStep = new LinkedHashMap<>();
        for (AgentWorkflowStep s : steps) {
            String key = s.getOutputKey() != null ? s.getOutputKey() : "step" + s.getStepOrder();
            keyToStep.put(key, s);
        }

        // 入度表
        Map<AgentWorkflowStep, Integer> inDegree = new HashMap<>();
        // 邻接表
        Map<AgentWorkflowStep, List<AgentWorkflowStep>> adjacency = new HashMap<>();

        for (AgentWorkflowStep s : steps) {
            inDegree.put(s, 0);
            adjacency.put(s, new ArrayList<>());
        }

        // 解析 depends_on，填充邻接表和入度
        for (AgentWorkflowStep s : steps) {
            List<String> deps = parseDependsOn(s.getDependsOn());
            for (String depKey : deps) {
                AgentWorkflowStep dep = keyToStep.get(depKey);
                if (dep != null && inDegree.containsKey(dep)) {
                    // dep → s（dep 执行完才能执行 s）
                    adjacency.get(dep).add(s);
                    inDegree.merge(s, 1, Integer::sum);
                } else {
                    log.warn("[Workflow] depends_on references unknown key '{}' for step {}",
                            depKey, s.getStepOrder());
                }
            }
        }

        // Kahn 算法，按层分组
        List<List<AgentWorkflowStep>> layers = new ArrayList<>();
        Queue<AgentWorkflowStep> queue = new LinkedList<>();

        // 入度为 0 的节点作为第一层
        for (AgentWorkflowStep s : steps) {
            if (inDegree.get(s) == 0) {
                queue.offer(s);
            }
        }

        while (!queue.isEmpty()) {
            List<AgentWorkflowStep> layer = new ArrayList<>();
            int layerSize = queue.size();

            for (int i = 0; i < layerSize; i++) {
                AgentWorkflowStep current = queue.poll();
                layer.add(current);

                for (AgentWorkflowStep next : adjacency.getOrDefault(current, Collections.emptyList())) {
                    int newDegree = inDegree.get(next) - 1;
                    inDegree.put(next, newDegree);
                    if (newDegree == 0) {
                        queue.offer(next);
                    }
                }
            }

            layers.add(layer);
        }

        // 检测循环依赖
        if (layers.stream().mapToInt(List::size).sum() != steps.size()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "工作流存在循环依赖，请检查 depends_on 配置");
        }

        return layers;
    }

    private List<String> parseDependsOn(String dependsOn) {
        if (dependsOn == null || dependsOn.isBlank()) return Collections.emptyList();
        try {
            return OM.readValue(dependsOn, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse depends_on JSON: {}", dependsOn);
            return Collections.emptyList();
        }
    }

    // ─────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────

    /**
     * 检查是否应该跳过当前步骤
     */
    private boolean shouldSkip(String skipCondition, String userInput, String lastOutput) {
        if (skipCondition == null || skipCondition.isBlank()) return false;

        String lower = skipCondition.toLowerCase();
        String inputLower = userInput != null ? userInput.toLowerCase() : "";
        String lastLower = lastOutput != null ? lastOutput.toLowerCase() : "";

        if (lower.startsWith("always:")) return true;
        if (lower.startsWith("contains:") && inputLower.contains(lower.substring(9).trim())) return true;
        if (lower.startsWith("not-contains:") && !inputLower.contains(lower.substring(14).trim())) return true;
        if (lower.startsWith("has-result:") && (lastOutput == null || lastOutput.isBlank())) return true;

        return false;
    }

    /**
     * 替换输入模板中的占位符
     * 支持：${input}, ${prev.output}, ${stepN.output}, ${context.KEY}
     */
    private String buildStepInput(String template, String userInput, String lastOutput,
            Map<String, String> context, int currentStep) {
        if (template == null || template.isBlank()) return userInput != null ? userInput : "";

        String result = template;
        result = result.replace("${input}", userInput != null ? userInput : "");
        result = result.replace("${prev.output}", lastOutput != null ? lastOutput : "");

        for (int i = 1; i <= currentStep - 1; i++) {
            String placeholder = "${step" + i + ".output}";
            String stepKey = "step" + i;
            result = result.replace(placeholder, context.getOrDefault(stepKey, ""));
        }

        for (Map.Entry<String, String> entry : context.entrySet()) {
            result = result.replace("${context." + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
        }

        return result;
    }

    private void persistContext(AgentWorkflowExecution execution, Map<String, String> context) {
        try {
            execution.setContextData(OM.writeValueAsString(context));
            executionRepository.save(execution);
        } catch (JsonProcessingException e) {
            log.warn("Failed to persist context: {}", e.getMessage());
        }
    }

    private String getStepKey(AgentWorkflowStep step) {
        return step.getOutputKey() != null ? step.getOutputKey() : "step" + step.getStepOrder();
    }

    // ─────────────────────────────────────────────
    // VO 转换
    // ─────────────────────────────────────────────

    private AgentWorkflowVO workflowToVO(AgentWorkflow w) {
        AgentWorkflowVO vo = new AgentWorkflowVO();
        vo.setId(w.getId());
        vo.setUserId(w.getUserId());
        vo.setName(w.getName());
        vo.setDescription(w.getDescription());
        vo.setVersion(w.getVersion());
        vo.setStatus(w.getStatus());
        vo.setCreatedAt(w.getCreateTime());
        vo.setUpdatedAt(w.getUpdateTime());

        List<AgentWorkflowStep> steps = stepRepository.findByWorkflowIdAndDeletedOrderByStepOrderAsc(w.getId(), 0);
        vo.setSteps(steps.stream().map(this::stepToVO).toList());
        return vo;
    }

    private AgentWorkflowStepVO stepToVO(AgentWorkflowStep s) {
        AgentWorkflowStepVO vo = new AgentWorkflowStepVO();
        vo.setId(s.getId());
        vo.setWorkflowId(s.getWorkflowId());
        vo.setStepOrder(s.getStepOrder());
        vo.setAgentId(s.getAgentId());
        vo.setStepName(s.getStepName());
        vo.setInputTemplate(s.getInputTemplate());
        vo.setOutputKey(s.getOutputKey());
        vo.setSkipCondition(s.getSkipCondition());
        vo.setDependsOn(s.getDependsOn());
        vo.setExecutionMode(s.getExecutionMode());
        vo.setRetryCount(s.getRetryCount());
        vo.setTimeoutSeconds(s.getTimeoutSeconds());
        return vo;
    }

    private AgentWorkflowExecutionVO execToVO(AgentWorkflowExecution e) {
        AgentWorkflowExecutionVO vo = new AgentWorkflowExecutionVO();
        vo.setId(e.getId());
        vo.setWorkflowId(e.getWorkflowId());
        vo.setUserId(e.getUserId());
        vo.setStatus(e.getStatus());
        vo.setCurrentStepOrder(e.getCurrentStepOrder());
        vo.setTotalSteps(e.getTotalSteps());
        vo.setStartTime(e.getStartTime() != null ? e.getStartTime().toString() : null);
        vo.setEndTime(e.getEndTime() != null ? e.getEndTime().toString() : null);
        vo.setErrorMessage(e.getErrorMessage());

        if (e.getStartTime() != null && e.getEndTime() != null) {
            vo.setDurationSeconds((e.getEndTime().getTime() - e.getStartTime().getTime()) / 1000);
        }

        // 状态标签
        vo.setStatusLabel(switch (e.getStatus()) {
            case 0 -> "进行中";
            case 1 -> "已完成";
            case 2 -> "失败";
            case 3 -> "已取消";
            default -> "未知";
        });

        // 加载工作流名称
        workflowRepository.findByIdAndDeleted(e.getWorkflowId(), 0)
                .ifPresent(w -> vo.setWorkflowName(w.getName()));

        // 解析上下文
        if (e.getContextData() != null && !e.getContextData().isBlank()) {
            try {
                vo.setContextData(OM.readValue(e.getContextData(), new TypeReference<Map<String, String>>() {}));
            } catch (JsonProcessingException ex) {
                log.warn("Failed to parse context_data: {}", ex.getMessage());
            }
        }

        return vo;
    }
}
