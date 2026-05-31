package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiModelService;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelAdminVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiModelSaveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "AI 模型配置")
@RestController
@RequestMapping("/api/v1/ai/admin/models")
public class AiModelController {

    @Resource
    private AiModelService aiModelService;

    @Operation(summary = "模型列表（管理端：掩码 apiKey，含 resolvedBaseUrl）")
    @PostMapping("/list")
    public RESTResult<List<AiModelAdminVO>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(aiModelService.listAllForAdmin());
    }

    @Operation(summary = "连通性测试（极简对话）")
    @PostMapping("/test-connection")
    public RESTResult<Map<String, Object>> testConnection(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        Long id = body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "id 不能为空");
        }
        return RESTResult.getSuccess(aiModelService.testConnection(id));
    }

    @Operation(summary = "保存模型（新增/编辑）")
    @PostMapping("/save")
    public RESTResult<Map<String, Object>> save(@Valid @RequestBody AiModelSaveVO vo, HttpServletRequest request) {
        requireAdmin(request);
        aiModelService.save(vo);
        // 勿用 getSuccess(null)：data 为空时业务 status=204，前端 axios 会按失败处理
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", true);
        if (vo.getId() != null) {
            payload.put("id", vo.getId());
        }
        return RESTResult.success("保存成功", payload, null);
    }

    @Operation(summary = "删除模型")
    @PostMapping("/delete")
    public RESTResult<Map<String, Object>> delete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        Long id = body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "id 不能为空");
        }
        aiModelService.delete(id);
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", true);
        payload.put("id", id);
        return RESTResult.success("删除成功", payload, null);
    }

    @Operation(summary = "设为默认模型")
    @PostMapping("/set-default")
    public RESTResult<Map<String, Object>> setDefault(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        Long id = body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "id 不能为空");
        }
        aiModelService.setDefault(id);
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", true);
        payload.put("id", id);
        return RESTResult.success("已设为默认", payload, null);
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }
}
