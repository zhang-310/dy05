package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveCompetitorScriptService;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 直播竞品话术库（C-4）
 */
@RestController
@RequestMapping("/api/v1/live/competitor-script")
@Tag(name = "直播竞品话术", description = "持久化竞品参考话术，owner 隔离")
public class LiveCompetitorScriptController {

    @Resource
    private LiveCompetitorScriptService competitorScriptService;

    @PostMapping("/search")
    @Operation(summary = "分页查询竞品话术")
    public RESTResult<PageResultVO<LiveCompetitorScriptVO>> search(HttpServletRequest request,
            @RequestBody(required = false) LiveCompetitorScriptSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) {
            vo = new LiveCompetitorScriptSearchVO();
        }
        PageResultVO<LiveCompetitorScriptVO> data = competitorScriptService.search(vo, userId);
        RESTResult<PageResultVO<LiveCompetitorScriptVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取详情")
    public RESTResult<LiveCompetitorScriptVO> get(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        RESTResult<LiveCompetitorScriptVO> r = RESTResult.getSuccess(competitorScriptService.getById(id, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody LiveCompetitorScriptSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        long id = competitorScriptService.save(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        competitorScriptService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
