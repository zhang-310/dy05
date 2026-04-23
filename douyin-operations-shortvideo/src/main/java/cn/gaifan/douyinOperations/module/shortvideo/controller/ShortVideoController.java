package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.service.PublishTimeRecommendationService;
import cn.gaifan.douyinOperations.module.shortvideo.service.impl.*;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/short-video")
@Tag(name = "短视频 / ShortVideo", description = "短视频管理（需登录）")
public class ShortVideoController {

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

    @Resource
    private SvVideoServiceImpl svVideoService;
    @Resource
    private SvCommentServiceImpl svCommentService;
    @Resource
    private SvCategoryServiceImpl svCategoryService;
    @Resource
    private DataScopeResolver dataScopeService;
    @Resource
    private PublishTimeRecommendationService publishTimeRecommendationService;
    @Resource
    private ContentCalendarService contentCalendarService;

    // ==================== 视频内容 ====================

    @PostMapping("/content/search")
    @Operation(summary = "分页搜索视频")
    public RESTResult<PageResultVO<SvVideoVO>> search(HttpServletRequest request,
            @RequestBody(required = false) SvVideoSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SvVideoSearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) vo.setOwnerIds(visibleIds);
        RESTResult<PageResultVO<SvVideoVO>> r = RESTResult.getSuccess(svVideoService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/get")
    @Operation(summary = "获取视频详情")
    public RESTResult<SvVideoVO> get(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        RESTResult<SvVideoVO> r = RESTResult.getSuccess(svVideoService.getById(id, visibleIds));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/save")
    @Operation(summary = "新增/更新视频")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody SvVideoSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        RESTResult<Long> r = RESTResult.addSuccess(svVideoService.save(vo, visibleIds));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/delete")
    @Operation(summary = "删除视频")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        svVideoService.delete(id, visibleIds);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/increment-view-count")
    @Operation(summary = "递增播放量")
    public RESTResult<Void> incrementViewCount(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        svVideoService.incrementViewCount(id, visibleIds);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/data-trend")
    @Operation(summary = "视频数据趋势（折线图）")
    public RESTResult<java.util.List<java.util.Map<String, Object>>> dataTrend(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long videoId = body != null && body.get("videoId") instanceof Number n ? n.longValue() : null;
        if (videoId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoId 必填");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        java.util.List<java.util.Map<String, Object>> list = svVideoService.getDataTrend(videoId, visibleIds);
        RESTResult<java.util.List<java.util.Map<String, Object>>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/calendar")
    @Operation(summary = "内容日历视图（按日分组计划与发布）")
    public RESTResult<java.util.Map<String, Object>> contentCalendar(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int year = body != null && body.get("year") instanceof Number n ? n.intValue() : java.time.LocalDate.now().getYear();
        int month = body != null && body.get("month") instanceof Number n ? n.intValue() : java.time.LocalDate.now().getMonthValue();
        if (month < 1 || month > 12) month = java.time.LocalDate.now().getMonthValue();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        java.util.Map<String, Object> data = contentCalendarService.getCalendarView(year, month, visibleIds);
        RESTResult<java.util.Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/calendar-stats")
    @Operation(summary = "内容日历统计（计划数/已发布数/完成率）")
    public RESTResult<java.util.Map<String, Object>> contentCalendarStats(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int year = body != null && body.get("year") instanceof Number n ? n.intValue() : java.time.LocalDate.now().getYear();
        int month = body != null && body.get("month") instanceof Number n ? n.intValue() : java.time.LocalDate.now().getMonthValue();
        if (month < 1 || month > 12) month = java.time.LocalDate.now().getMonthValue();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        java.util.Map<String, Object> data = contentCalendarService.getCalendarStats(year, month, visibleIds);
        RESTResult<java.util.Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/content/publish-time-recommend")
    @Operation(summary = "发布时间推荐（基于历史数据）")
    public RESTResult<java.util.List<java.util.Map<String, Object>>> publishTimeRecommend(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long accountId = body != null && body.get("accountId") instanceof Number n ? n.longValue() : null;
        if (accountId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "accountId 必填");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        java.util.List<java.util.Map<String, Object>> list = publishTimeRecommendationService.getRecommendedTimes(accountId, visibleIds);
        RESTResult<java.util.List<java.util.Map<String, Object>>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 分类 ====================

    @PostMapping("/category/list")
    @Operation(summary = "获取分类列表")
    public RESTResult<List<SvCategoryVO>> categoryList(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<SvCategoryVO>> r = RESTResult.getSuccess(svCategoryService.listByOwner(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/category/get")
    @Operation(summary = "获取分类详情")
    public RESTResult<SvCategoryVO> categoryGet(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        RESTResult<SvCategoryVO> r = RESTResult.getSuccess(svCategoryService.getById(id, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/category/save")
    @Operation(summary = "新增/更新分类")
    public RESTResult<Long> categorySave(HttpServletRequest request, @Valid @RequestBody SvCategorySaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(svCategoryService.save(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/category/delete")
    @Operation(summary = "删除分类")
    public RESTResult<Void> categoryDelete(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        svCategoryService.delete(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 评论 ====================

    @PostMapping("/comment/search")
    @Operation(summary = "分页搜索评论")
    public RESTResult<PageResultVO<SvCommentVO>> commentSearch(HttpServletRequest request,
            @RequestBody(required = false) SvCommentSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SvCommentSearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        RESTResult<PageResultVO<SvCommentVO>> r = RESTResult.getSuccess(svCommentService.search(vo, visibleIds));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/comment/get")
    @Operation(summary = "获取评论详情")
    public RESTResult<SvCommentVO> commentGet(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        RESTResult<SvCommentVO> r = RESTResult.getSuccess(svCommentService.getById(id, visibleIds));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/comment/save")
    @Operation(summary = "新增/更新评论")
    public RESTResult<Long> commentSave(HttpServletRequest request, @Valid @RequestBody SvCommentSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        RESTResult<Long> r = RESTResult.addSuccess(svCommentService.save(vo, visibleIds));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/comment/delete")
    @Operation(summary = "删除评论")
    public RESTResult<Void> commentDelete(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        svCommentService.delete(id, visibleIds);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/comment/increment-like-count")
    @Operation(summary = "评论点赞+1")
    public RESTResult<Void> commentLike(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = longFromBody(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        svCommentService.incrementLikeCount(id, visibleIds);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
