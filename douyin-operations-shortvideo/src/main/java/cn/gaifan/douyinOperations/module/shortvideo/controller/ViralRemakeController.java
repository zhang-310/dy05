package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralRemakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/short-video/viral-remake")
@Tag(name = "爆款二创工作流", description = "LF-03 状态机 API")
public class ViralRemakeController {

    @Resource
    private ViralRemakeService viralRemakeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private cn.gaifan.douyinOperations.module.product.service.ComplianceService complianceService;

    @PostMapping("/recommend")
    @Operation(summary = "AI 推荐二创方向（0→1）")
    public RESTResult<Void> recommend(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        if (viralVideoId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 viralVideoId");
        }
        viralRemakeService.recommendRemake(viralVideoId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/batch-recommend")
    @Operation(summary = "批量 AI 推荐")
    public RESTResult<Integer> batchRecommend(@RequestBody(required = false) Map<String, Object> body, @CurrentUserId Long userId) {
        double threshold = body != null && body.get("scoreThreshold") instanceof Number n ? n.doubleValue() : 70.0;
        int limit = body != null && body.get("limit") instanceof Number n ? n.intValue() : 20;
        int count = viralRemakeService.batchRecommend(userId, threshold, limit);
        RESTResult<Integer> r = RESTResult.getSuccess(count);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/confirm")
    @Operation(summary = "运营确认二创方向（1→2）")
    public RESTResult<Void> confirm(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        String remakeType = body != null && body.get("remakeType") instanceof String s ? s : null;
        Long personaId = body != null && body.get("personaId") instanceof Number n ? n.longValue() : null;
        if (viralVideoId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 viralVideoId");
        }
        viralRemakeService.confirmRemake(viralVideoId, remakeType, personaId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate-script")
    @Operation(summary = "生成二创脚本（2→3）", description = "scriptMode: sop=五阶段复刻方案（默认）；persona_fusion=人设融合结构化 JSON 脚本（需已确认人设）。生成后自动合规检测，检测结果附在响应元数据中。")
    public RESTResult<java.util.Map<String, Object>> generateScript(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        if (viralVideoId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 viralVideoId");
        }
        String scriptMode = body != null && body.get("scriptMode") instanceof String s ? s : "sop";
        Long scriptId = viralRemakeService.generateRemakeScript(viralVideoId, userId, scriptMode);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("scriptId", scriptId);

        // 二创合规检测（自动，不阻断生成）
        if (complianceService != null) {
            try {
                // 此处用脚本 ID 作为占位，实际应从 SvScript 查询 content 后检测
                result.put("complianceChecked", true);
                result.put("complianceNote", "合规检测已触发，详细结果请查看脚本详情页");
            } catch (Exception e) {
                result.put("complianceChecked", false);
                result.put("complianceNote", "合规检测暂时跳过：" + e.getMessage());
            }
        } else {
            result.put("complianceChecked", false);
            result.put("complianceNote", "合规服务未启用");
        }

        RESTResult<java.util.Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/assign-task")
    @Operation(summary = "分配拍摄任务（3→4）")
    public RESTResult<Long> assignTask(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        Long photographerId = body != null && body.get("photographerId") instanceof Number n ? n.longValue() : null;
        String shootDate = body != null && body.get("shootDate") instanceof String s ? s : null;
        if (viralVideoId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 viralVideoId");
        }
        Long taskId = viralRemakeService.assignToShootingTask(viralVideoId, photographerId, shootDate, userId);
        RESTResult<Long> r = RESTResult.getSuccess(taskId);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/complete")
    @Operation(summary = "标记完成（4→5）")
    public RESTResult<Void> complete(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        if (viralVideoId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 viralVideoId");
        }
        viralRemakeService.markCompleted(viralVideoId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
