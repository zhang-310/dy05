package cn.gaifan.douyinOperations.module.workflow.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.workflow.entity.WorkflowDefinition;
import cn.gaifan.douyinOperations.module.workflow.entity.WorkflowStep;
import cn.gaifan.douyinOperations.module.workflow.repository.WorkflowDefinitionRepository;
import cn.gaifan.douyinOperations.module.workflow.repository.WorkflowStepRepository;
import cn.gaifan.douyinOperations.module.workflow.service.WorkflowExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowExecutorImpl implements WorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutorImpl.class);

    @Resource
    private WorkflowDefinitionRepository definitionRepository;
    @Resource
    private WorkflowStepRepository stepRepository;
    @Resource
    private LiveAiService liveAiService;
    @Resource
    private LiveScriptService liveScriptService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowExecuteResult execute(String workflowCode, Map<String, Object> params) {
        if (workflowCode == null || workflowCode.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "workflowCode 不能为空");
        }

        // P0-1: 提前提取 userId 用于数据隔离校验
        Long userId = paramLong(params, "userId");
        Long sessionId = paramLong(params, "sessionId");
        if (userId == null || sessionId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "缺少 userId 或 sessionId");
        }

        // P0-1: 强制数据隔离 - 只能执行自己创建的工作流
        WorkflowDefinition def = definitionRepository.findByWorkflowCodeAndOwnerIdAndDeleted(workflowCode, userId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流不存在或无权限"));

        List<WorkflowStep> steps = stepRepository.findByDefinitionIdAndDeletedOrderBySequenceNoAsc(def.getId(), 0);
        if (steps.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "工作流无步骤配置");
        }

        Map<String, Object> outputs = new LinkedHashMap<>();

        List<Long> scriptIds = new ArrayList<>();
        try {
            for (WorkflowStep step : steps) {
                String stepCode = step.getStepCode();
                switch (stepCode) {
                    case "generate" -> {
                        LiveAiGenerateVO vo = new LiveAiGenerateVO();
                        vo.setSessionId(sessionId);
                        vo.setPersonaId(paramLong(params, "personaId"));
                        vo.setModelId(paramLong(params, "modelId"));
                        LiveAiFullResultVO full = liveAiService.generateFull(vo);
                        if (full != null && full.getScriptIdsForAttribution() != null) {
                            scriptIds.clear();
                            scriptIds.addAll(full.getScriptIdsForAttribution());
                        }
                        outputs.put("generate", Map.of("scriptIds", new ArrayList<>(scriptIds), "results", full != null && full.getResults() != null ? full.getResults() : List.of()));
                    }
                    case "iterate" -> {
                        String instruction = paramStr(params, "iterateInstruction");
                        Long iterateScriptId = paramLong(params, "iterateScriptId");
                        if (instruction != null && !instruction.isBlank()) {
                            Long targetId = iterateScriptId != null ? iterateScriptId : (scriptIds.isEmpty() ? null : scriptIds.get(0));
                            if (targetId != null) {
                                String refined = liveAiService.refineScript(targetId, instruction, userId, paramLong(params, "modelId"));
                                outputs.put("iterate", Map.of("scriptId", targetId, "refined", refined));
                            }
                        }
                    }
                    case "violation_check" -> {
                        List<Map<String, Object>> results = new ArrayList<>();
                        for (Long sid : scriptIds) {
                            var check = liveAiService.checkViolation(userId, sid);
                            results.add(Map.of("scriptId", sid, "passed", check != null && check.isPassed(), "violations", check != null ? check.getViolations() : List.of()));
                        }
                        outputs.put("violation_check", results);
                        boolean allPassed = results.stream().allMatch(r -> Boolean.TRUE.equals(r.get("passed")));
                        if (!allPassed) {
                            return new WorkflowExecuteResult(false, "违规检测未通过", outputs, "violation_check");
                        }
                    }
                    case "save" -> {
                        liveScriptService.ensureScriptSlotsForSession(sessionId, userId);
                        var slots = liveScriptService.getBySessionId(sessionId, userId);
                        outputs.put("save", Map.of("count", slots != null ? slots.size() : 0));
                    }
                    default -> log.warn("未知步骤: {}", stepCode);
                }
            }
            return new WorkflowExecuteResult(true, "执行成功", outputs, null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // P1-2: 敏感参数日志脱敏
            log.error("工作流执行失败: workflowCode={}, params={}, error={}",
                workflowCode, sanitizeParams(params), e.getMessage(), e);
            throw new BusinessException(ErrorCode.WORKFLOW_EXECUTION_FAILED, "工作流执行失败: " + e.getMessage());
        }
    }

    // P1-2: 参数脱敏方法
    private Map<String, Object> sanitizeParams(Map<String, Object> params) {
        if (params == null) return null;
        Map<String, Object> sanitized = new LinkedHashMap<>(params);
        List<String> sensitiveKeys = List.of("password", "token", "apiKey", "secret", "accessKey", "accessToken");

        for (String key : sensitiveKeys) {
            if (sanitized.containsKey(key)) {
                sanitized.put(key, "***");
            }
        }
        return sanitized;
    }

    private static Long paramLong(Map<String, Object> params, String key) {
        if (params == null) return null;
        Object v = params.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String paramStr(Map<String, Object> params, String key) {
        if (params == null) return null;
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }
}
