package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiAgentWorkflowContext;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiAgentWorkflowContextRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.AgentOutputValidator;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.MultiAgentOrchestrator;
import cn.gaifan.douyinOperations.module.ai.vo.AgentNodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 多 Agent 协同编排器 — DAG 并行引擎
 * <p>
 * 使用 Kahn 拓扑排序 + CompletableFuture 实现并行执行。
 * DAG 拓扑：
 * ProductAnalyst → ScriptWriter → ComplianceChecker
 * ProductAnalyst → ScheduleOptimizer
 * （ScriptWriter 与 ScheduleOptimizer 可并行执行）
 * <p>
 * 每节点 60s 超时 + 2 次指数退避重试。
 * 节点失败不阻塞无依赖分支。
 */
@Slf4j
@Service
public class MultiAgentOrchestratorImpl implements MultiAgentOrchestrator {

    private static final long NODE_TIMEOUT_SECONDS = 60;
    private static final int MAX_RETRIES = 2;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Autowired(required = false)
    private AgentVotingServiceImpl agentVotingService;

    @Autowired(required = false)
    private AgentOutputValidator agentOutputValidator;

    @Autowired(required = false)
    private AiAgentWorkflowContextRepository workflowContextRepository;

    private static final Map<String, String> AGENT_SYSTEM_PROMPTS = Map.of(
            "ProductAnalyst", "你是产品分析师Agent。分析商品数据，推荐主推品、引流品、利润品的最优组合。返回JSON格式：{\"recommendation\":{\"mainProduct\":{},\"trafficProducts\":[],\"profitProducts\":[]},\"reasoning\":\"\"}",
            "ScriptWriter", "你是话术撰写Agent。根据产品分析结果和排品建议，为每个商品生成对应的直播话术。返回JSON：{\"scripts\":[{\"productName\":\"\",\"opening\":\"\",\"keyPoints\":[],\"closingCta\":\"\"}]}",
            "ComplianceChecker", "你是合规审核Agent。审核话术中是否有违反广告法、平台规则的内容。返回JSON：{\"passed\":true/false,\"violations\":[{\"text\":\"\",\"rule\":\"\",\"suggestion\":\"\"}]}",
            "ScheduleOptimizer", "你是排品优化Agent。根据产品分析结果，优化商品展示顺序和时间分配。返回JSON：{\"schedule\":[{\"position\":1,\"productName\":\"\",\"duration\":\"3min\",\"role\":\"引流\"}],\"strategy\":\"\"}"
    );

    /** DAG 边定义：key 依赖于 value 列表中的所有节点 */
    private static final Map<String, List<String>> DAG_DEPENDENCIES = Map.of(
            "ProductAnalyst", List.of(),
            "ScriptWriter", List.of("ProductAnalyst"),
            "ComplianceChecker", List.of("ScriptWriter"),
            "ScheduleOptimizer", List.of("ProductAnalyst")
    );

    @Override
    public Map<String, Object> executeWorkflow(Long sessionId, Long userId) {
        String workflowId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("[MultiAgent] 开始 DAG 并行编排: sessionId={}, workflowId={}", sessionId, workflowId);
        Map<String, Object> result = new LinkedHashMap<>();

        if (llmClient == null || aiModelRepository == null) {
            result.put("error", "AI服务不可用");
            return result;
        }

        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) {
            result.put("error", "无可用AI模型");
            return result;
        }
        AiModel model = models.get(0);

        String contextPrompt = "直播场次ID: " + sessionId + "，请分析当前场次的商品数据并给出建议。";

        // Kahn 拓扑排序 + CompletableFuture 并行执行
        Map<String, AgentNodeResult> nodeResults = new ConcurrentHashMap<>();
        Map<String, CompletableFuture<AgentNodeResult>> futures = new HashMap<>();

        long workflowStart = System.currentTimeMillis();

        // 按层级执行
        List<List<String>> layers = kahnTopologicalSort();
        for (List<String> layer : layers) {
            List<CompletableFuture<AgentNodeResult>> layerFutures = new ArrayList<>();

            for (String role : layer) {
                // 初始化节点上下文
                persistNodeContext(workflowId, sessionId, role, "RUNNING", null, null, null);

                CompletableFuture<AgentNodeResult> future = CompletableFuture.supplyAsync(() -> {
                    String inputPrompt = buildNodeInput(role, contextPrompt, nodeResults);
                    return executeNodeWithRetry(role, inputPrompt, model, sessionId);
                });

                futures.put(role, future);
                layerFutures.add(future);
            }

            // 等待当前层所有节点完成
            try {
                CompletableFuture.allOf(layerFutures.toArray(new CompletableFuture[0]))
                        .get(NODE_TIMEOUT_SECONDS * layer.size(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("[MultiAgent] 层级等待超时: {}", e.getMessage());
            }

            // 收集当前层结果并持久化
            for (String role : layer) {
                CompletableFuture<AgentNodeResult> f = futures.get(role);
                try {
                    AgentNodeResult nr = f.getNow(AgentNodeResult.failed(role, "超时", 0, NODE_TIMEOUT_SECONDS * 1000, 0));
                    nodeResults.put(role, nr);
                    persistNodeResult(workflowId, sessionId, role, nr);
                } catch (Exception e) {
                    AgentNodeResult failed = AgentNodeResult.failed(role, e.getMessage(), 0, 0, 0);
                    nodeResults.put(role, failed);
                    persistNodeResult(workflowId, sessionId, role, failed);
                }
            }
        }

        long workflowDuration = System.currentTimeMillis() - workflowStart;

        // 构建返回结果
        Map<String, Object> nodeDetail = new LinkedHashMap<>();
        for (Map.Entry<String, AgentNodeResult> entry : nodeResults.entrySet()) {
            AgentNodeResult nr = entry.getValue();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("output", nr.getOutput());
            detail.put("status", nr.getStatus());
            detail.put("latencyMs", nr.getLatencyMs());
            detail.put("inputSize", nr.getInputSize());
            detail.put("outputSize", nr.getOutputSize());
            detail.put("retryCount", nr.getRetryCount());
            if (nr.getErrorMessage() != null) detail.put("error", nr.getErrorMessage());
            nodeDetail.put(entry.getKey(), detail);
        }

        AgentNodeResult analysisResult = nodeResults.get("ProductAnalyst");
        AgentNodeResult scriptResult = nodeResults.get("ScriptWriter");
        AgentNodeResult complianceResult = nodeResults.get("ComplianceChecker");
        AgentNodeResult scheduleResult = nodeResults.get("ScheduleOptimizer");

        result.put("workflowId", workflowId);
        result.put("productAnalysis", analysisResult != null ? analysisResult.getOutput() : null);
        result.put("scripts", scriptResult != null ? scriptResult.getOutput() : null);
        result.put("complianceReport", complianceResult != null ? complianceResult.getOutput() : null);
        result.put("schedule", scheduleResult != null ? scheduleResult.getOutput() : null);
        result.put("nodeExecutionDetails", nodeDetail);
        result.put("totalDurationMs", workflowDuration);
        result.put("success", true);

        log.info("[MultiAgent] DAG 并行编排完成: sessionId={}, workflowId={}, duration={}ms", sessionId, workflowId, workflowDuration);
        return result;
    }

    /**
     * 断点续跑：恢复未完成的工作流，跳过已成功的节点
     */
    public Map<String, Object> resumeWorkflow(String workflowId) {
        if (workflowContextRepository == null) {
            return Map.of("error", "工作流上下文存储不可用");
        }

        List<AiAgentWorkflowContext> contexts = workflowContextRepository.findByWorkflowIdAndDeleted(workflowId, 0);
        if (contexts.isEmpty()) {
            return Map.of("error", "工作流不存在: " + workflowId);
        }

        Long sessionId = contexts.get(0).getSessionId();
        log.info("[MultiAgent] 断点续跑: workflowId={}, sessionId={}", workflowId, sessionId);

        if (llmClient == null || aiModelRepository == null) {
            return Map.of("error", "AI服务不可用");
        }
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) {
            return Map.of("error", "无可用AI模型");
        }
        AiModel model = models.get(0);

        // 恢复已完成节点的结果
        Map<String, AgentNodeResult> nodeResults = new ConcurrentHashMap<>();
        Set<String> completedNodes = new HashSet<>();
        for (AiAgentWorkflowContext ctx : contexts) {
            if ("SUCCESS".equals(ctx.getNodeStatus())) {
                completedNodes.add(ctx.getNodeRole());
                AgentNodeResult restored = AgentNodeResult.success(
                        ctx.getNodeRole(), ctx.getOutputJson(), 0, 0);
                nodeResults.put(ctx.getNodeRole(), restored);
                log.info("[MultiAgent] 跳过已完成节点: {}", ctx.getNodeRole());
            }
        }

        String contextPrompt = "直播场次ID: " + sessionId + "，请分析当前场次的商品数据并给出建议。";
        Map<String, CompletableFuture<AgentNodeResult>> futures = new HashMap<>();
        long workflowStart = System.currentTimeMillis();

        List<List<String>> layers = kahnTopologicalSort();
        for (List<String> layer : layers) {
            List<CompletableFuture<AgentNodeResult>> layerFutures = new ArrayList<>();

            for (String role : layer) {
                if (completedNodes.contains(role)) continue;

                persistNodeContext(workflowId, sessionId, role, "RUNNING", null, null, null);

                CompletableFuture<AgentNodeResult> future = CompletableFuture.supplyAsync(() -> {
                    String inputPrompt = buildNodeInput(role, contextPrompt, nodeResults);
                    return executeNodeWithRetry(role, inputPrompt, model, sessionId);
                });
                futures.put(role, future);
                layerFutures.add(future);
            }

            if (!layerFutures.isEmpty()) {
                try {
                    CompletableFuture.allOf(layerFutures.toArray(new CompletableFuture[0]))
                            .get(NODE_TIMEOUT_SECONDS * layerFutures.size(), TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("[MultiAgent] 层级等待超时: {}", e.getMessage());
                }
            }

            for (String role : layer) {
                if (completedNodes.contains(role)) continue;
                CompletableFuture<AgentNodeResult> f = futures.get(role);
                if (f != null) {
                    try {
                        AgentNodeResult nr = f.getNow(AgentNodeResult.failed(role, "超时", 0, NODE_TIMEOUT_SECONDS * 1000, 0));
                        nodeResults.put(role, nr);
                        persistNodeResult(workflowId, sessionId, role, nr);
                    } catch (Exception e) {
                        AgentNodeResult failed = AgentNodeResult.failed(role, e.getMessage(), 0, 0, 0);
                        nodeResults.put(role, failed);
                        persistNodeResult(workflowId, sessionId, role, failed);
                    }
                }
            }
        }

        long workflowDuration = System.currentTimeMillis() - workflowStart;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workflowId", workflowId);
        result.put("resumed", true);
        result.put("skippedNodes", completedNodes);
        result.put("productAnalysis", getNodeOutput(nodeResults, "ProductAnalyst"));
        result.put("scripts", getNodeOutput(nodeResults, "ScriptWriter"));
        result.put("complianceReport", getNodeOutput(nodeResults, "ComplianceChecker"));
        result.put("schedule", getNodeOutput(nodeResults, "ScheduleOptimizer"));
        result.put("totalDurationMs", workflowDuration);
        result.put("success", true);

        log.info("[MultiAgent] 断点续跑完成: workflowId={}, skipped={}, duration={}ms",
                workflowId, completedNodes, workflowDuration);
        return result;
    }

    private String getNodeOutput(Map<String, AgentNodeResult> results, String role) {
        AgentNodeResult r = results.get(role);
        return r != null ? r.getOutput() : null;
    }

    /** 持久化节点初始上下文 */
    private void persistNodeContext(String workflowId, Long sessionId, String role,
                                    String status, String inputJson, String outputJson, String errorMsg) {
        if (workflowContextRepository == null) return;
        try {
            AiAgentWorkflowContext ctx = new AiAgentWorkflowContext();
            ctx.setSessionId(sessionId);
            ctx.setWorkflowId(workflowId);
            ctx.setNodeRole(role);
            ctx.setNodeStatus(status);
            ctx.setInputJson(inputJson);
            ctx.setOutputJson(outputJson);
            ctx.setErrorMessage(errorMsg);
            ctx.setStartedAt(new Timestamp(System.currentTimeMillis()));
            workflowContextRepository.save(ctx);
        } catch (Exception e) {
            log.warn("[MultiAgent] 持久化节点上下文失败: role={}, err={}", role, e.getMessage());
        }
    }

    /** 持久化节点执行结果 */
    private void persistNodeResult(String workflowId, Long sessionId, String role, AgentNodeResult nr) {
        if (workflowContextRepository == null) return;
        try {
            // 查找已有记录并更新
            List<AiAgentWorkflowContext> existing = workflowContextRepository.findByWorkflowIdAndDeleted(workflowId, 0);
            AiAgentWorkflowContext ctx = existing.stream()
                    .filter(c -> role.equals(c.getNodeRole()) && !"SUCCESS".equals(c.getNodeStatus()))
                    .findFirst()
                    .orElse(null);
            if (ctx == null) return;

            ctx.setNodeStatus(nr.isSuccess() ? "SUCCESS" : "FAILED");
            ctx.setOutputJson(nr.getOutput());
            ctx.setErrorMessage(nr.getErrorMessage());
            ctx.setRetryCount(nr.getRetryCount());
            ctx.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            workflowContextRepository.save(ctx);
        } catch (Exception e) {
            log.warn("[MultiAgent] 持久化节点结果失败: role={}, err={}", role, e.getMessage());
        }
    }

    /**
     * Kahn 拓扑排序 → 返回层级列表（同层可并行）
     */
    private List<List<String>> kahnTopologicalSort() {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (String node : DAG_DEPENDENCIES.keySet()) {
            inDegree.putIfAbsent(node, 0);
            adjacency.putIfAbsent(node, new ArrayList<>());
        }

        for (Map.Entry<String, List<String>> entry : DAG_DEPENDENCIES.entrySet()) {
            String node = entry.getKey();
            for (String dep : entry.getValue()) {
                adjacency.computeIfAbsent(dep, k -> new ArrayList<>()).add(node);
                inDegree.merge(node, 1, Integer::sum);
            }
        }

        List<List<String>> layers = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();

        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        while (!queue.isEmpty()) {
            List<String> layer = new ArrayList<>();
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String node = queue.poll();
                layer.add(node);
                for (String next : adjacency.getOrDefault(node, List.of())) {
                    inDegree.merge(next, -1, Integer::sum);
                    if (inDegree.get(next) == 0) queue.add(next);
                }
            }
            layers.add(layer);
        }

        return layers;
    }

    /**
     * 构建节点输入：根据依赖的前置节点结果拼接 prompt
     */
    private String buildNodeInput(String role, String contextPrompt, Map<String, AgentNodeResult> nodeResults) {
        return switch (role) {
            case "ProductAnalyst" -> contextPrompt;
            case "ScriptWriter" -> {
                AgentNodeResult analysis = nodeResults.get("ProductAnalyst");
                yield "产品分析结果:\n" + (analysis != null && analysis.isSuccess() ? analysis.getOutput() : "分析失败，请自行推断")
                        + "\n\n请为以上推荐商品生成直播话术。";
            }
            case "ComplianceChecker" -> {
                AgentNodeResult scripts = nodeResults.get("ScriptWriter");
                yield "请审核以下话术的合规性:\n" + (scripts != null && scripts.isSuccess() ? scripts.getOutput() : "话术生成失败");
            }
            case "ScheduleOptimizer" -> {
                AgentNodeResult analysis = nodeResults.get("ProductAnalyst");
                yield "产品分析结果:\n" + (analysis != null && analysis.isSuccess() ? analysis.getOutput() : "分析失败，请自行推断")
                        + "\n\n请优化排品顺序。";
            }
            default -> contextPrompt;
        };
    }

    /**
     * 带指数退避重试的节点执行
     */
    private AgentNodeResult executeNodeWithRetry(String role, String inputPrompt, AiModel model, Long sessionId) {
        long startMs = System.currentTimeMillis();
        int retryCount = 0;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    long backoffMs = (long) Math.pow(2, attempt) * 1000;
                    Thread.sleep(backoffMs);
                    retryCount = attempt;
                }

                String output;
                // ProductAnalyst 使用投票机制（如果可用）
                if ("ProductAnalyst".equals(role) && agentVotingService != null) {
                    output = agentVotingService.voteOnProductAnalysis(inputPrompt, model);
                } else {
                    output = executeAgent(role, inputPrompt, model);
                }

                // 输出校验与自修复（如果可用）
                if (agentOutputValidator != null && output != null) {
                    output = agentOutputValidator.validateAndRepair(role, output, inputPrompt, model, sessionId);
                }

                long latencyMs = System.currentTimeMillis() - startMs;
                return AgentNodeResult.success(role, output, inputPrompt.length(), latencyMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("[MultiAgent] {} 第{}次执行失败: {}", role, attempt + 1, e.getMessage());
            }
        }

        long latencyMs = System.currentTimeMillis() - startMs;
        return AgentNodeResult.failed(role, "重试耗尽", inputPrompt.length(), latencyMs, retryCount);
    }

    String executeAgent(String agentRole, String userPrompt, AiModel model) {
        String systemPrompt = AGENT_SYSTEM_PROMPTS.getOrDefault(agentRole, "");
        var resp = llmClient.chat(model, systemPrompt, userPrompt);
        if (resp != null && resp.success()) {
            return resp.content();
        }
        return "{\"error\":\"" + agentRole + " Agent执行失败\"}";
    }
}
