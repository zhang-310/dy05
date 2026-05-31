package cn.gaifan.douyinOperations.module.photoavatar.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.photoavatar.service.PhotoAvatarService;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSaveVO;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/photo-avatar")
@Tag(name = "照片转视频 / Photo Avatar")
public class PhotoAvatarController {

    @Resource
    private PhotoAvatarService service;

    @PostMapping("/overview")
    @Operation(summary = "照片转视频概览")
    public RESTResult<Map<String, Object>> overview(HttpServletRequest req) {
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(service.overview(uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/search")
    @Operation(summary = "分页搜索任务列表")
    public RESTResult<?> search(@RequestBody PhotoAvatarSearchVO vo, HttpServletRequest req) {
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<?> r = RESTResult.getSuccess(service.search(vo, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/create")
    @Operation(summary = "创建视频生成任务")
    public RESTResult<Long> create(@RequestBody PhotoAvatarSaveVO vo, HttpServletRequest req) {
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<Long> r = RESTResult.addSuccess(service.createVideo(vo, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/status")
    @Operation(summary = "查询任务详情")
    public RESTResult<?> status(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : 0L;
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<?> r = RESTResult.getSuccess(service.getStatus(id, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "软删除任务")
    public RESTResult<Void> delete(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : 0L;
        Long uid = AuthTokenFilter.getUserId(req);
        service.delete(id, uid != null ? uid : 0L);
        RESTResult<Void> r = RESTResult.success(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/retry")
    @Operation(summary = "重试失败任务")
    public RESTResult<Long> retry(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : 0L;
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<Long> r = RESTResult.addSuccess(service.retry(id, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
