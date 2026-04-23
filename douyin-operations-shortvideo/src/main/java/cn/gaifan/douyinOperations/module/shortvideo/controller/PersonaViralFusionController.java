package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.PersonaViralFusionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LF-05 爆款 × 人设融合、热点三要素
 */
@RestController
@RequestMapping("/api/v1/short-video/persona-fusion")
@Tag(name = "爆款人设融合", description = "LF-05 Persona × Viral")
public class PersonaViralFusionController {

    @Resource
    private PersonaViralFusionService fusionService;

    @PostMapping("/match-personas")
    @Operation(summary = "为爆款匹配人设")
    public RESTResult<List<Map<String, Object>>> matchPersonas(
            @RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        if (viralVideoId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 viralVideoId");
        }
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(
                fusionService.matchPersonas(viralVideoId, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate-fused-script")
    @Operation(summary = "爆款×人设 融合生成脚本")
    public RESTResult<Map<String, Object>> generateFusedScript(
            @RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        Long personaId = body != null && body.get("personaId") instanceof Number n ? n.longValue() : null;
        if (viralVideoId == null || personaId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 viralVideoId 或 personaId");
        }
        String remakeType = body != null && body.get("remakeType") instanceof String s ? s : "form_imitation";
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(
                fusionService.generatePersonaFusedScript(viralVideoId, personaId, remakeType, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate-hotspot-fused")
    @Operation(summary = "热点×人设×产品 融合")
    public RESTResult<Map<String, Object>> generateHotspotFused(
            @RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long hotTopicId = body != null && body.get("hotTopicId") instanceof Number n ? n.longValue() : null;
        Long personaId = body != null && body.get("personaId") instanceof Number n ? n.longValue() : null;
        Long productId = body != null && body.get("productId") instanceof Number n ? n.longValue() : null;
        if (hotTopicId == null || personaId == null) {
            return RESTResult.validError(ErrorCode.VALIDATION_FAIL, "缺少 hotTopicId 或 personaId");
        }
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(
                fusionService.generateHotspotFusedScript(hotTopicId, personaId, productId, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
