package cn.gaifan.douyinOperations.module.drama.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.drama.service.DramaService;
import cn.gaifan.douyinOperations.module.drama.vo.DramaSaveVO;
import cn.gaifan.douyinOperations.module.drama.vo.DramaSearchVO;
import cn.gaifan.douyinOperations.module.drama.vo.DramaUpdateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("appDramaController")
@RequestMapping("/api/v1/drama")
@Tag(name = "短剧制作 / Drama")
public class DramaController {

    @Resource
    private DramaService service;

    @PostMapping("/overview")
    @Operation(summary = "短剧概览")
    public RESTResult<Map<String, Object>> overview(HttpServletRequest req) {
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(service.overview(uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/search")
    @Operation(summary = "分页搜索短剧项目")
    public RESTResult<?> search(@RequestBody DramaSearchVO vo, HttpServletRequest req) {
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<?> r = RESTResult.getSuccess(service.search(vo, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/create")
    @Operation(summary = "创建短剧项目")
    public RESTResult<Long> create(@RequestBody DramaSaveVO vo, HttpServletRequest req) {
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<Long> r = RESTResult.addSuccess(service.createProject(vo, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update")
    @Operation(summary = "更新短剧项目")
    public RESTResult<Long> update(@RequestBody DramaUpdateVO vo, HttpServletRequest req) {
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<Long> r = RESTResult.addSuccess(service.update(vo, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/detail")
    @Operation(summary = "获取项目详情")
    public RESTResult<?> detail(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : 0L;
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<?> r = RESTResult.getSuccess(service.getDetail(id, uid != null ? uid : 0L));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "软删除短剧项目")
    public RESTResult<Void> delete(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : 0L;
        Long uid = AuthTokenFilter.getUserId(req);
        service.delete(id, uid != null ? uid : 0L);
        RESTResult<Void> r = RESTResult.success(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/change-status")
    @Operation(summary = "变更项目状态")
    public RESTResult<Void> changeStatus(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : 0L;
        String newStatus = body.get("status") instanceof String s ? s : "";
        Long uid = AuthTokenFilter.getUserId(req);
        service.changeStatus(id, newStatus, uid != null ? uid : 0L);
        RESTResult<Void> r = RESTResult.success(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/export-to-maker")
    @Operation(summary = "导出到短视频成片（drama→shortvideo-maker 互调）")
    public RESTResult<Map<String, Object>> exportToMaker(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long id = body.get("id") instanceof Number n ? n.longValue() : 0L;
        String traceId = body.get("traceId") instanceof String s ? s : MDC.get("traceId");
        Long uid = AuthTokenFilter.getUserId(req);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(
                service.exportToShortvideoMaker(id, uid != null ? uid : 0L, traceId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
