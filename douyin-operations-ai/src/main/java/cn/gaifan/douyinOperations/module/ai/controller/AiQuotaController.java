package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "AI 额度")
@RestController
@RequestMapping("/api/v1/ai/admin/quota")
public class AiQuotaController {

    @Resource
    private AiQuotaService aiQuotaService;

    @Operation(summary = "获取配额信息")
    @PostMapping("/get")
    public RESTResult<Map<String, Object>> getQuota(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        return RESTResult.getSuccess(aiQuotaService.getAdminQuotaOverview());
    }

    @Operation(summary = "更新配额上限")
    @PostMapping("/update")
    public RESTResult<Void> updateQuota(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        aiQuotaService.updateQuotaLimits(body, getUserId(request));
        return RESTResult.getSuccess(null);
    }

    @Operation(summary = "配额历史记录")
    @PostMapping("/history")
    public RESTResult<Map<String, Object>> quotaHistory(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        requireAdmin(request);
        int page = body.get("page") instanceof Number n ? n.intValue() : 0;
        int rows = body.get("rows") instanceof Number n ? n.intValue() : 20;
        String feature = body.get("feature") instanceof String s ? s : null;
        var result = aiQuotaService.getQuotaHistory(page, rows, feature);
        return RESTResult.getSuccess(Map.of(
                "list", result.getList(),
                "total", result.getTotal(),
                "pageNum", result.getPageNum(),
                "pageSize", result.getPageSize()
        ));
    }

    @Operation(summary = "获取当前用户当日额度信息（用户端）")
    @PostMapping("/info")
    public RESTResult<Map<String, Object>> getQuotaInfo(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        AiQuotaService.QuotaInfo info = aiQuotaService.getQuota(userId);
        return RESTResult.getSuccess(Map.of(
                "usedCount", info.usedCount(),
                "maxCount", info.maxCount(),
                "remaining", info.remaining()
        ));
    }

    private void requireAdmin(HttpServletRequest request) {
        String roleCode = (String) request.getAttribute("roleCode");
        if (roleCode == null || !"admin".equals(roleCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }
}
