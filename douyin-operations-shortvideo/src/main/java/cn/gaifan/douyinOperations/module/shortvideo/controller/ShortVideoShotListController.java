package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyShootService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotListVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 短视频分镜 API
 * 路径：/api/v1/short-video/shot-list
 */
@RestController
@RequestMapping("/api/v1/short-video/shot-list")
@Tag(name = "短视频分镜", description = "分镜生成、保存")
public class ShortVideoShotListController {

    @Resource
    private SvShotListService shotListService;

    @Resource
    private DailyShootService dailyShootService;

    @PostMapping("/list")
    @Operation(summary = "分镜列表")
    public RESTResult<PageResultVO<SvShotListVO>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Integer page = body != null && body.get("page") instanceof Number n ? n.intValue() : 0;
        Integer rows = body != null && body.get("rows") instanceof Number n ? n.intValue() : 20;
        PageResultVO<SvShotListVO> result = shotListService.list(userId, page, rows);
        RESTResult<PageResultVO<SvShotListVO>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "分镜详情")
    public RESTResult<SvShotListVO> get(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        SvShotListVO vo = shotListService.get(id, userId);
        RESTResult<SvShotListVO> r = RESTResult.getSuccess(vo);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get-by-script")
    @Operation(summary = "根据脚本获取最新分镜")
    public RESTResult<SvShotListVO> getByScript(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long scriptId = body != null && body.get("scriptId") instanceof Number n ? n.longValue() : null;
        if (scriptId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 scriptId");
        SvShotListVO vo = shotListService.getLatestByScriptId(scriptId, userId);
        RESTResult<SvShotListVO> r = RESTResult.getSuccess(vo);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存分镜")
    public RESTResult<Long> save(@RequestBody SvShotSaveVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = shotListService.save(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete-shot")
    @Operation(summary = "删除单条分镜")
    public RESTResult<Void> deleteShot(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long shotId = body != null && body.get("shotId") instanceof Number n ? n.longValue() : null;
        if (shotId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "shotId 不能为空");
        shotListService.deleteShot(shotId, userId);
        RESTResult<Void> r = RESTResult.success();
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate")
    @Operation(summary = "AI 生成分镜")
    public RESTResult<Map<String, Object>> generate(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long scriptId = body != null && body.get("scriptId") instanceof Number n ? n.longValue() : null;
        String scriptContent = body != null && body.get("scriptContent") instanceof String s ? s : null;
        Integer shotCount = body != null && body.get("shotCount") instanceof Number n ? n.intValue() : null;
        String style = body != null && body.get("style") instanceof String s ? s : null;
        if (scriptContent == null || scriptContent.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "scriptContent 不能为空");
        SvShotListService.GenerateResult gen = shotListService.generateWithResult(scriptId, scriptContent, shotCount, style, userId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("shots", gen.shots());
        if (gen.shotListId() != null) data.put("shotListId", gen.shotListId());
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/review")
    @Operation(summary = "分镜审核（每日拍摄）")
    public RESTResult<Map<String, Object>> review(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sceneId = body != null && body.get("sceneId") instanceof Number n ? n.longValue() : null;
        String reviewStatus = body != null && body.get("reviewStatus") instanceof String s ? s : null;
        String reviewerNote = body != null && body.get("reviewerNote") instanceof String s ? s : null;
        if (sceneId == null || !"approved".equals(reviewStatus) && !"needs_revision".equals(reviewStatus))
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "sceneId 和 reviewStatus(approved/needs_revision) 必填");
        Map<String, Object> result = dailyShootService.reviewShot(userId, sceneId, reviewStatus, reviewerNote);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
