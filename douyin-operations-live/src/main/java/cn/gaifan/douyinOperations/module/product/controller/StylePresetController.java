package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.product.entity.StylePreset;
import cn.gaifan.douyinOperations.module.product.service.StylePresetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风格预设模板管理 Controller
 */
@RestController
@RequestMapping("/api/v1/product/style-preset")
@Tag(name = "风格预设 / Style Preset", description = "话术风格预设管理（需登录）")
public class StylePresetController {

    @Resource
    private StylePresetService stylePresetService;

    @PostMapping("/list")
    @Operation(summary = "获取启用的风格列表（供生成对话框使用）/ List Enabled Styles")
    public RESTResult<List<StylePreset>> listEnabled(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        List<StylePreset> list = stylePresetService.listEnabled();
        RESTResult<List<StylePreset>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/list-all")
    @Operation(summary = "获取全部风格（管理用）/ List All Styles")
    public RESTResult<List<StylePreset>> listAll(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        List<StylePreset> list = stylePresetService.listAll();
        RESTResult<List<StylePreset>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取风格详情 / Get Style Preset Detail")
    public RESTResult<StylePreset> getById(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null ? body.get("id") : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        StylePreset preset = stylePresetService.getById(id);
        RESTResult<StylePreset> r = RESTResult.getSuccess(preset);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存风格预设 / Save Style Preset")
    public RESTResult<StylePreset> save(HttpServletRequest request,
                                        @RequestBody StylePreset vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        StylePreset preset = stylePresetService.save(vo, userId);
        RESTResult<StylePreset> r = RESTResult.getSuccess(preset);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除风格预设（软删）/ Delete Style Preset")
    public RESTResult<Void> delete(HttpServletRequest request,
                                   @RequestBody java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") != null ? ((Number) body.get("id")).longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        stylePresetService.delete(id, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除风格预设（软删，REST风格）/ Delete Style Preset (REST)")
    public RESTResult<Void> deleteRest(HttpServletRequest request,
                                   @Parameter(description = "风格 ID", required = true)
                                   @PathVariable Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        stylePresetService.delete(id, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/recommend")
    @Operation(summary = "智能推荐风格 / Recommend Styles for Product")
    public RESTResult<List<String>> recommend(HttpServletRequest request,
                                               @RequestBody java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long productId = body != null && body.get("productId") != null ? ((Number) body.get("productId")).longValue() : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");

        List<String> recommended = stylePresetService.recommendStyles(productId, userId);
        RESTResult<List<String>> r = RESTResult.getSuccess(recommended);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
