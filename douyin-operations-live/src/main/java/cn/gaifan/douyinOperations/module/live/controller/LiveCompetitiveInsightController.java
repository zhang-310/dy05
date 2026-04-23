package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.service.LiveCompetitiveInsightService;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightVO;
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
 * 直播竞品商业对比快照（C-2/C-3/C-5 MVP：手工录入）
 */
@RestController
@RequestMapping("/api/v1/live/competitive-insight")
@Tag(name = "直播竞品洞察", description = "定价·份额·GMV·胜负备注，owner 隔离")
public class LiveCompetitiveInsightController {

    @Resource
    private LiveCompetitiveInsightService insightService;

    @PostMapping("/search")
    @Operation(summary = "分页查询竞品洞察")
    public RESTResult<PageResultVO<LiveCompetitiveInsightVO>> search(HttpServletRequest request,
            @RequestBody(required = false) LiveCompetitiveInsightSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) {
            vo = new LiveCompetitiveInsightSearchVO();
        }
        RESTResult<PageResultVO<LiveCompetitiveInsightVO>> r =
                RESTResult.getSuccess(insightService.search(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取详情")
    public RESTResult<LiveCompetitiveInsightVO> get(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        }
        RESTResult<LiveCompetitiveInsightVO> r = RESTResult.getSuccess(insightService.getById(id, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody LiveCompetitiveInsightSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        RESTResult<Long> r = RESTResult.addSuccess(insightService.save(vo, userId));
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
        insightService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
