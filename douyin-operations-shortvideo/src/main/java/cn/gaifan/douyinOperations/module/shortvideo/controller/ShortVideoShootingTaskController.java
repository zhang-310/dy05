package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShootingTaskService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 短视频拍摄任务工单 API（五主播管线 MVP）
 * 路径：/api/v1/short-video/shooting-task
 */
@RestController
@RequestMapping("/api/v1/short-video/shooting-task")
@Tag(name = "短视频拍摄任务", description = "运营派单、摄影师/主播可见（需登录）")
public class ShortVideoShootingTaskController {

    @Resource
    private SvShootingTaskService shootingTaskService;

    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/list")
    @Operation(summary = "拍摄任务分页列表")
    public RESTResult<PageResultVO<SvShootingTaskVO>> list(@RequestBody(required = false) SvShootingTaskSearchVO vo,
                                                           HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SvShootingTaskSearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        PageResultVO<SvShootingTaskVO> data = shootingTaskService.search(vo, userId, visibleIds);
        RESTResult<PageResultVO<SvShootingTaskVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "拍摄任务详情")
    public RESTResult<SvShootingTaskVO> get(@RequestBody(required = false) Map<String, Object> body,
                                             HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        SvShootingTaskVO data = shootingTaskService.get(id, userId, visibleIds);
        RESTResult<SvShootingTaskVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存拍摄任务（新建或更新，仅创建人可改）")
    public RESTResult<Long> save(@RequestBody @Valid SvShootingTaskSaveVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = shootingTaskService.save(vo, userId);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除拍摄任务（逻辑删除，仅创建人）")
    public RESTResult<Void> delete(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        shootingTaskService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
