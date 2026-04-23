package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 爆款库控制器
 */
@RestController
@RequestMapping("/api/v1/short-video/viral")
@Tag(name = "爆款库 / Viral Video Library", description = "短视频爆款库管理")
public class ViralVideoController {

    @Resource
    private ViralVideoService viralVideoService;

    private static Long longFromBody(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return null;
        Object v = body.get(key);
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取爆款列表（JSON Body：mode, category, keyword→category, sortBy, page, rows）
     */
    @PostMapping("/list")
    @Operation(summary = "爆款列表 / List Viral Videos")
    public RESTResult<List<SvViralVideo>> list(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String mode = "my";
        String category = null;
        String sortBy = "viralScore";
        int page = 0;
        int rows = 20;
        if (body != null) {
            if (body.get("mode") instanceof String s && !s.isBlank()) mode = s;
            if (body.get("category") instanceof String s && !s.isBlank()) category = s;
            if (category == null && body.get("keyword") instanceof String s && !s.isBlank()) category = s;
            if (body.get("sortBy") instanceof String s && !s.isBlank()) sortBy = s;
            if (body.get("page") instanceof Number n) page = n.intValue();
            if (body.get("rows") instanceof Number n) rows = n.intValue();
        }
        if (rows < 1) rows = 20;
        if (rows > 100) rows = 100;
        if (page < 0) page = 0;

        List<SvViralVideo> list = viralVideoService.listViralVideos(userId, mode, category, sortBy, page, rows);
        RESTResult<List<SvViralVideo>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 收藏爆款视频
     */
    @PostMapping("/collect")
    @Operation(summary = "收藏爆款 / Collect Viral Video")
    public RESTResult<Long> collect(@RequestBody ViralCollectVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        Long id = viralVideoService.collectViralVideo(vo, userId);
        RESTResult<Long> r = RESTResult.getSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取爆款详情
     */
    @PostMapping("/get")
    @Operation(summary = "爆款详情 / Get Viral Video")
    public RESTResult<SvViralVideo> get(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");

        SvViralVideo viral = viralVideoService.getViralVideo(id, userId);
        RESTResult<SvViralVideo> r = RESTResult.getSuccess(viral);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 删除收藏
     */
    @PostMapping("/delete")
    @Operation(summary = "删除收藏 / Delete Viral Video")
    public RESTResult<Map<String, Object>> delete(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");

        viralVideoService.deleteViralVideo(id, userId);
        // 勿用 getSuccess(null)：data 为 null 时 JSON 内 status=204，前端 axios 会按失败处理
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", true);
        payload.put("id", id);
        RESTResult<Map<String, Object>> r = RESTResult.success("删除成功", payload, null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 触发AI分析
     */
    @PostMapping("/analyze")
    @Operation(summary = "AI分析 / Analyze Viral Video")
    public RESTResult<Map<String, Object>> analyze(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");

        viralVideoService.triggerAnalysis(id, userId);
        // 必须返回非空 data：前端 unwrap result.data；null 易被误判；且勿用 getSuccess(null)→204
        Map<String, Object> payload = new HashMap<>();
        payload.put("ok", true);
        payload.put("id", id);
        RESTResult<Map<String, Object>> r = RESTResult.success("分析任务已触发", payload, null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 爆款复刻
     */
    @PostMapping("/replicate")
    @Operation(summary = "爆款复刻 / Replicate Viral Video")
    public RESTResult<String> replicate(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");

        String plan = viralVideoService.replicateViral(id, userId);
        RESTResult<String> r = RESTResult.getSuccess(plan);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取推荐爆款
     */
    @PostMapping("/recommended")
    @Operation(summary = "推荐爆款 / Recommended Viral Videos")
    public RESTResult<List<SvViralVideo>> recommended(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        int limit = 10;
        if (body != null && body.get("limit") instanceof Number n) {
            limit = n.intValue();
        }
        if (limit < 1) limit = 10;
        if (limit > 50) limit = 50;

        List<SvViralVideo> list = viralVideoService.getRecommendedVirals(userId, limit);
        RESTResult<List<SvViralVideo>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
