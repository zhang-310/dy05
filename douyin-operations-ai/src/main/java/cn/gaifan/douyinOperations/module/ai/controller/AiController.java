package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.impl.AiServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI 管理 / AI", description = "AI 模型与生成任务管理（需登录）")
public class AiController {

    @Resource
    private AiServiceImpl aiService;

    // ==================== 模型管理 ====================

    @PostMapping("/model/list")
    @Operation(summary = "AI 模型列表")
    public RESTResult<List<Map<String, Object>>> modelList(HttpServletRequest request,
            @RequestParam(required = false) Integer status) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(aiService.listModels(status));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/model/get")
    @Operation(summary = "AI 模型详情")
    public RESTResult<Map<String, Object>> modelGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(aiService.getModelById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/model/save")
    @Operation(summary = "新增/更新 AI 模型（Admin）")
    public RESTResult<Long> modelSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body.get("id") != null ? Long.parseLong(body.get("id").toString()) : null;
        String modelName = (String) body.get("modelName");
        String modelProvider = (String) body.get("modelProvider");
        String modelVersion = (String) body.get("modelVersion");
        if (modelName == null || modelProvider == null || modelVersion == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "modelName/modelProvider/modelVersion 不能为空");
        }
        Integer maxTokens = body.get("maxTokens") != null ? Integer.parseInt(body.get("maxTokens").toString()) : null;
        Double temperature = body.get("temperature") != null ? Double.parseDouble(body.get("temperature").toString()) : null;
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;
        RESTResult<Long> r = RESTResult.addSuccess(aiService.saveModel(id, modelName, modelProvider, modelVersion,
                (String) body.get("apiKey"), maxTokens, temperature, status));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/model/delete")
    @Operation(summary = "删除 AI 模型（Admin）")
    public RESTResult<Void> modelDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        aiService.deleteModel(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 生成任务 ====================

    @PostMapping("/task/list")
    @Operation(summary = "生成任务列表（分页）")
    public RESTResult<PageResultVO<Map<String, Object>>> taskList(HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int rows,
            @RequestParam(required = false) String taskType,
            @RequestParam(required = false) Integer taskStatus) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(
                aiService.searchTasks(userId, taskType, taskStatus, page, rows));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/task/get")
    @Operation(summary = "生成任务详情")
    public RESTResult<Map<String, Object>> taskGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(aiService.getTaskById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/task/create")
    @Operation(summary = "创建生成任务")
    public RESTResult<Long> taskCreate(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String taskType = (String) body.get("taskType");
        if (taskType == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "taskType 不能为空");
        RESTResult<Long> r = RESTResult.addSuccess(aiService.createTask(userId, taskType,
                (String) body.get("inputContent"), (String) body.get("prompt"), (String) body.get("modelUsed")));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/task/complete")
    @Operation(summary = "完成生成任务（内部回调）")
    public RESTResult<Void> taskComplete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = Long.parseLong(body.get("id").toString());
        String output = (String) body.get("outputContent");
        Long tokens = body.get("tokensUsed") != null ? Long.parseLong(body.get("tokensUsed").toString()) : 0L;
        aiService.completeTask(id, output, tokens);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== Prompt 模板 ====================

    @PostMapping("/prompt/list")
    @Operation(summary = "Prompt 模板列表")
    public RESTResult<List<Map<String, Object>>> promptList(HttpServletRequest request,
            @RequestParam(required = false) String category) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(aiService.listPromptTemplates(userId, category));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/prompt/save")
    @Operation(summary = "新增/更新 Prompt 模板")
    public RESTResult<Long> promptSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body.get("id") != null ? Long.parseLong(body.get("id").toString()) : null;
        String templateName = (String) body.get("templateName");
        String templateContent = (String) body.get("templateContent");
        if (templateName == null || templateContent == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "templateName 和 templateContent 不能为空");
        }
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;
        RESTResult<Long> r = RESTResult.addSuccess(aiService.savePromptTemplate(userId, id, templateName, templateContent,
                (String) body.get("category"), (String) body.get("variables"), status));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/prompt/delete")
    @Operation(summary = "删除 Prompt 模板")
    public RESTResult<Void> promptDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        aiService.deletePromptTemplate(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 知识库 ====================

    @PostMapping("/knowledge/list")
    @Operation(summary = "知识库列表")
    public RESTResult<List<Map<String, Object>>> knowledgeList(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(aiService.listKnowledgeBases(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/knowledge/get")
    @Operation(summary = "知识库详情")
    public RESTResult<Map<String, Object>> knowledgeGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(aiService.getKnowledgeBaseById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/knowledge/save")
    @Operation(summary = "新增/更新知识库")
    public RESTResult<Long> knowledgeSave(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body.get("id") != null ? Long.parseLong(body.get("id").toString()) : null;
        String kbName = (String) body.get("kbName");
        if (kbName == null || kbName.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "kbName 不能为空");
        RESTResult<Long> r = RESTResult.addSuccess(aiService.saveKnowledgeBase(userId, id, kbName,
                (String) body.get("description"), (String) body.get("embeddingModel")));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/knowledge/delete")
    @Operation(summary = "删除知识库")
    public RESTResult<Void> knowledgeDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        aiService.deleteKnowledgeBase(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/knowledge/status")
    @Operation(summary = "更新知识库状态")
    public RESTResult<Void> knowledgeStatus(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = Long.parseLong(body.get("id").toString());
        Integer status = Integer.parseInt(body.get("status").toString());
        aiService.updateKnowledgeBaseStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
