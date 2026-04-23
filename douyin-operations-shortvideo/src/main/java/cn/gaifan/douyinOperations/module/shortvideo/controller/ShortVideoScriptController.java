package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 短视频脚本 API
 * 路径：/api/v1/short-video/script
 */
@RestController
@RequestMapping("/api/v1/short-video/script")
@Tag(name = "短视频脚本", description = "脚本 CRUD、AI 生成、爆款分析")
public class ShortVideoScriptController {

    @Resource
    private SvScriptService scriptService;

    @PostMapping("/list")
    @Operation(summary = "脚本列表")
    public RESTResult<PageResultVO<SvScriptVO>> list(@RequestBody SvScriptSearchVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        PageResultVO<SvScriptVO> result = scriptService.search(vo, userId);
        RESTResult<PageResultVO<SvScriptVO>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "脚本详情")
    public RESTResult<SvScriptVO> get(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        SvScriptVO vo = scriptService.get(id, userId);
        RESTResult<SvScriptVO> r = RESTResult.getSuccess(vo);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存脚本")
    public RESTResult<Long> save(@RequestBody @Valid SvScriptSaveVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = scriptService.save(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除脚本")
    public RESTResult<Void> delete(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        scriptService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate")
    @Operation(summary = "AI 生成脚本")
    public RESTResult<String> generate(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String type = body != null && body.get("type") instanceof String s ? s : "daily";
        String theme = body != null && body.get("theme") instanceof String s ? s : null;
        Long viralVideoId = body != null && body.get("viralVideoId") instanceof Number n ? n.longValue() : null;
        String productInfo = body != null && body.get("productInfo") instanceof String s ? s : null;
        String style = body != null && body.get("style") instanceof String s ? s : null;
        Integer duration = body != null && body.get("duration") instanceof Number n ? n.intValue() : null;
        String content = scriptService.generate(type, theme, viralVideoId, productInfo, style, duration, userId);
        RESTResult<String> r = RESTResult.getSuccess(content);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/analyze-viral")
    @Operation(summary = "分析爆款脚本", description = "主入口；data/analyze-viral 为其别名")
    public RESTResult<String> analyzeViral(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String url = body != null && body.get("viralVideoUrl") instanceof String s ? s : null;
        String level = body != null && body.get("extractLevel") instanceof String s ? s : "basic";
        if (url == null || url.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "viralVideoUrl 不能为空");
        String result = scriptService.analyzeViral(url, level, userId);
        RESTResult<String> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
