package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.entity.ScriptVersionHistory;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptService;
import cn.gaifan.douyinOperations.module.product.service.ScriptVersionHistoryService;
import cn.gaifan.douyinOperations.module.product.vo.BatchGenerateRequestVO;
import cn.gaifan.douyinOperations.module.product.vo.MultiStyleGenerateRequestVO;
import cn.gaifan.douyinOperations.module.product.vo.MultiStyleGenerateResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptSaveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 产品话术管理 Controller
 */
@RestController
@RequestMapping("/api/v1/product/script")
@Tag(name = "产品话术管理 / Product Script", description = "产品多套话术管理（需登录）")
public class ProductScriptController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductScriptController.class);

    @Resource
    private ProductScriptService productScriptService;

    @Resource
    private ScriptVersionHistoryService scriptVersionHistoryService;

    @PostMapping("/save")
    @Operation(summary = "保存产品话术 / Save Product Script")
    public RESTResult<DyProductScript> saveScript(HttpServletRequest request,
                                                   @Valid @RequestBody ProductScriptSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        DyProductScript script = productScriptService.saveScript(vo, userId);
        RESTResult<DyProductScript> r = RESTResult.getSuccess(script);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/list")
    @Operation(summary = "获取产品所有话术 / List Product Scripts")
    public RESTResult<List<DyProductScript>> listScripts(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");
        List<DyProductScript> scripts = productScriptService.listScripts(productId, userId);
        RESTResult<List<DyProductScript>> r = RESTResult.getSuccess(scripts);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/search")
    @Operation(summary = "搜索产品话术（别名：/list）/ Search Product Scripts")
    public RESTResult<List<DyProductScript>> searchScripts(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        // 直接调用 listScripts，提供别名支持
        return listScripts(request, body);
    }

    @PostMapping("/list-by-type")
    @Operation(summary = "获取产品指定类型的话术 / List Scripts By Type")
    public RESTResult<List<DyProductScript>> listScriptsByType(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        String scriptType = body != null && body.get("scriptType") != null ? body.get("scriptType").toString() : null;
        if (productId == null || scriptType == null || scriptType.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId 或 scriptType");
        List<DyProductScript> scripts = productScriptService.listScriptsByType(productId, scriptType, userId);
        RESTResult<List<DyProductScript>> r = RESTResult.getSuccess(scripts);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/list-by-style")
    @Operation(summary = "按风格分组列出话术 / List Scripts By Style")
    public RESTResult<Map<String, List<DyProductScript>>> listScriptsByStyle(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        String scriptType = body != null && body.get("scriptType") != null ? body.get("scriptType").toString() : null;
        if (productId == null || scriptType == null || scriptType.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId 或 scriptType");
        Map<String, List<DyProductScript>> map = productScriptService.listScriptsByStyle(productId, scriptType, userId);
        RESTResult<Map<String, List<DyProductScript>>> r = RESTResult.getSuccess(map);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/active-by-style")
    @Operation(summary = "按风格获取各风格激活话术 / Get Active Scripts By Style")
    public RESTResult<Map<String, DyProductScript>> getActiveScriptsByStyle(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        String scriptType = body != null && body.get("scriptType") != null ? body.get("scriptType").toString() : null;
        if (productId == null || scriptType == null || scriptType.isBlank())
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId 或 scriptType");
        Map<String, DyProductScript> map = productScriptService.getActiveScriptsByStyle(productId, scriptType, userId);
        RESTResult<Map<String, DyProductScript>> r = RESTResult.getSuccess(map);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate-multi-style")
    @Operation(summary = "AI 多风格生成 / Generate Multi-Style Scripts")
    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "aiGenerate", fallbackMethod = "generateFallback")
    public RESTResult<MultiStyleGenerateResultVO> generateMultiStyle(HttpServletRequest request,
                                                                      @Valid @RequestBody MultiStyleGenerateRequestVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        MultiStyleGenerateResultVO result = productScriptService.generateMultiStyleScripts(vo, userId);
        RESTResult<MultiStyleGenerateResultVO> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // P1-2: 限流降级方法
    private RESTResult<MultiStyleGenerateResultVO> generateFallback(HttpServletRequest request,
                                                                     MultiStyleGenerateRequestVO vo,
                                                                     io.github.resilience4j.ratelimiter.RequestNotPermitted ex) {
        return RESTResult.error(ErrorCode.RATE_LIMIT, "AI 生成请求过于频繁，请稍后再试");
    }

    @GetMapping(value = "/generate-multi-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 多风格生成（SSE 进度推送）/ Generate Multi-Style Scripts with SSE")
    public SseEmitter generateMultiStyleSse(HttpServletRequest request,
                                            @RequestParam Long productId,
                                            @RequestParam String styles,
                                            @RequestParam(required = false) String scriptType,
                                            @RequestParam(required = false) Boolean fusionMode,
                                            @RequestParam(required = false) String styleWeights,
                                            @RequestParam(required = false) String fusionStrategy,
                                            @RequestParam(required = false) Long personaId,
                                            @RequestParam(required = false) Integer duration,
                                            @RequestParam(required = false) String scene,
                                            @RequestParam(required = false) Boolean useKbRef,
                                            @RequestParam(required = false) String kbCategories) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            SseEmitter err = new SseEmitter(1000L);
            err.completeWithError(new RuntimeException("未登录"));
            return err;
        }

        MultiStyleGenerateRequestVO vo = new MultiStyleGenerateRequestVO();
        vo.setProductId(productId);
        vo.setScriptType(scriptType != null ? scriptType : "formal");
        vo.setStyles(List.of(styles.split(",")));
        vo.setFusionMode(fusionMode);
        vo.setFusionStrategy(fusionStrategy);
        vo.setPersonaId(personaId);
        vo.setDuration(duration);
        vo.setScene(scene);
        vo.setUseKbRef(useKbRef);
        if (kbCategories != null && !kbCategories.isBlank()) {
            vo.setKbCategories(List.of(kbCategories.split(",")));
        }
        if (styleWeights != null && !styleWeights.isBlank()) {
            try {
                vo.setStyleWeights(parseStyleWeights(styleWeights));
            } catch (Exception e) {
                SseEmitter err = new SseEmitter(1000L);
                err.completeWithError(new RuntimeException("styleWeights 格式错误"));
                return err;
            }
        }

        SseEmitter emitter = new SseEmitter(600_000L);
        productScriptService.generateMultiStyleScriptsWithProgress(vo, userId, emitter);
        return emitter;
    }

    private Map<String, Double> parseStyleWeights(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Invalid styleWeights JSON: " + e.getMessage());
        }
    }

    @PostMapping(value = "/generate-batch-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "批量生成（SSE 进度推送）/ Batch Generate with SSE Progress")
    public SseEmitter generateBatchStream(HttpServletRequest request,
                                          @Valid @RequestBody BatchGenerateRequestVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            SseEmitter err = new SseEmitter(1000L);
            err.completeWithError(new RuntimeException("未登录"));
            return err;
        }
        SseEmitter emitter = new SseEmitter(600_000L); // 10 分钟
        try {
            productScriptService.generateBatchWithProgress(vo, userId, (current, total, productId, productName, style, success, message) -> {
                try {
                    Map<String, Object> data = Map.of(
                            "current", current,
                            "total", total,
                            "percent", total > 0 ? (int) (100.0 * current / total) : 0,
                            "productId", productId != null ? productId : 0,
                            "productName", productName != null ? productName : "",
                            "style", style != null ? style : "",
                            "success", success,
                            "message", message != null ? message : ""
                    );
                    emitter.send(SseEmitter.event().name("progress").data(data, MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(Map.of("error", e.getMessage()), MediaType.APPLICATION_JSON));
                    } catch (Exception e2) { log.debug("SSE error通知失败: {}", e2.getMessage()); }
                }
            });
            emitter.send(SseEmitter.event().name("done").data(Map.of("status", "ok"), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            try {
                String msg = e.getMessage() != null ? e.getMessage() : "批量生成失败";
                emitter.send(SseEmitter.event().name("error").data(Map.of("error", msg), MediaType.APPLICATION_JSON));
            } catch (Exception e2) { log.debug("SSE error通知失败: {}", e2.getMessage()); }
        } finally {
            emitter.complete();
        }
        return emitter;
    }

    @PostMapping("/active")
    @Operation(summary = "获取产品的激活话术 / List Active Scripts")
    public RESTResult<List<DyProductScript>> listActiveScripts(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");
        List<DyProductScript> scripts = productScriptService.listActiveScripts(productId, userId);
        RESTResult<List<DyProductScript>> r = RESTResult.getSuccess(scripts);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PutMapping("/activate/{scriptId}")
    @Operation(summary = "激活话术 / Activate Script")
    public RESTResult<Void> activateScript(HttpServletRequest request,
                                            @Parameter(description = "话术 ID", required = true)
                                            @PathVariable Long scriptId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        productScriptService.activateScript(scriptId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PutMapping("/update/{scriptId}")
    @Operation(summary = "更新话术 / Update Script")
    public RESTResult<DyProductScript> updateScript(HttpServletRequest request,
                                                     @Parameter(description = "话术 ID", required = true)
                                                     @PathVariable Long scriptId,
                                                     @Valid @RequestBody ProductScriptSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        DyProductScript script = productScriptService.updateScript(scriptId, vo, userId);
        RESTResult<DyProductScript> r = RESTResult.getSuccess(script);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @DeleteMapping("/{scriptId}")
    @Operation(summary = "删除话术 / Delete Script")
    public RESTResult<Void> deleteScript(HttpServletRequest request,
                                          @Parameter(description = "话术 ID", required = true)
                                          @PathVariable Long scriptId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        productScriptService.deleteScript(scriptId, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取话术详情 / Get Script Detail")
    public RESTResult<DyProductScript> getScript(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long scriptId = body != null && body.get("id") != null ? ((Number) body.get("id")).longValue() : null;
        if (scriptId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        DyProductScript script = productScriptService.getScript(scriptId, userId);
        RESTResult<DyProductScript> r = RESTResult.getSuccess(script);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate")
    @Operation(summary = "AI生成单个话术 / Generate Single Script")
    public RESTResult<DyProductScript> generateScript(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");

        String style = body != null && body.get("style") != null ? body.get("style").toString() : "专业";

        try {
            DyProductScript script = productScriptService.generateSingleScript(productId, style, userId);
            RESTResult<DyProductScript> r = RESTResult.getSuccess(script);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/usage-list")
    @Operation(summary = "获取话术使用统计 / Get Script Usage Statistics")
    public RESTResult<Map<String, Object>> getScriptUsageList(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");

        try {
            Map<String, Object> usageStats = productScriptService.getScriptUsageStatistics(productId, userId);
            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(usageStats);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "获取统计失败: " + e.getMessage());
        }
    }

    @PostMapping("/statistics")
    @Operation(summary = "获取话术统计信息 / Get Script Statistics")
    public RESTResult<Map<String, Object>> getScriptStatistics(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");

        try {
            Map<String, Object> statistics = productScriptService.getScriptStatistics(productId, userId);
            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(statistics);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "获取统计失败: " + e.getMessage());
        }
    }

    @PostMapping("/detail")
    @Operation(summary = "获取话术详情 / Get Script Detail")
    public RESTResult<DyProductScript> getScriptDetail(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long scriptId = body != null && body.get("scriptId") != null ? ((Number) body.get("scriptId")).longValue() : null;
        if (scriptId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scriptId");
        DyProductScript script = productScriptService.getScript(scriptId, userId);
        RESTResult<DyProductScript> r = RESTResult.getSuccess(script);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/history")
    @Operation(summary = "获取话术版本历史 / List Script Version History")
    public RESTResult<List<ScriptVersionHistory>> listVersionHistory(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long scriptId = body != null && body.get("scriptId") != null ? ((Number) body.get("scriptId")).longValue() : null;
        if (scriptId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scriptId");
        productScriptService.getScript(scriptId, userId);
        List<ScriptVersionHistory> list = scriptVersionHistoryService.listByScriptId(scriptId);
        RESTResult<List<ScriptVersionHistory>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/rollback/{historyId}")
    @Operation(summary = "回滚到指定版本 / Rollback to Version")
    public RESTResult<DyProductScript> rollbackToVersion(HttpServletRequest request,
                                                          @Parameter(description = "版本历史 ID", required = true)
                                                          @PathVariable Long historyId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        DyProductScript script = productScriptService.rollbackToVersion(historyId, userId);
        RESTResult<DyProductScript> r = RESTResult.getSuccess(script);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/preview-styles")
    @Operation(summary = "预览多个风格的话术片段 / Preview Styles")
    public RESTResult<Map<String, Object>> previewStyles(HttpServletRequest request,
                                                          @Valid @RequestBody MultiStyleGenerateRequestVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        List<cn.gaifan.douyinOperations.module.product.vo.StylePreviewVO> previews =
                productScriptService.previewStyles(vo, userId);

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of("previews", previews));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
