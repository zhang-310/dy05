package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.service.TaskModelConfigService;
import cn.gaifan.douyinOperations.module.ai.vo.TaskModelConfigRowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务-模型配置 API（管理员）
 */
@Tag(name = "AI 任务-模型配置")
@RestController
@RequestMapping("/api/v1/ai/admin/task-model-config")
public class TaskModelConfigController {

    @Resource
    private TaskModelConfigService taskModelConfigService;

    @Operation(summary = "任务-模型配置列表")
    @PostMapping("/list")
    public RESTResult<List<TaskModelConfigRowVO>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(taskModelConfigService.listAll());
    }

    @Operation(summary = "任务-模型配置详情")
    @PostMapping("/get")
    public RESTResult<TaskModelConfigRowVO> get(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "id 不能为空");
        return RESTResult.getSuccess(taskModelConfigService.getRowById(id));
    }

    @Operation(summary = "新增/更新任务-模型配置")
    @PostMapping("/save")
    public RESTResult<Long> save(@RequestBody AiTaskModelConfig config, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.addSuccess(taskModelConfigService.save(config));
    }

    @Operation(summary = "删除任务-模型配置")
    @PostMapping("/delete")
    public RESTResult<Map<String, Object>> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "id 不能为空");
        taskModelConfigService.delete(id);
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", true);
        payload.put("id", id);
        return RESTResult.success("删除成功", payload, null);
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
