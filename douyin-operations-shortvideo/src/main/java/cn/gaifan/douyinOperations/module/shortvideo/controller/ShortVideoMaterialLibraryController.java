package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.MaterialLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 短视频素材库 API
 * 路径：/api/v1/short-video/library
 */
@RestController
@RequestMapping("/api/v1/short-video/library")
@Tag(name = "短视频素材库", description = "素材列表、删除")
public class ShortVideoMaterialLibraryController {

    @Resource
    private MaterialLibraryService materialLibraryService;

    @PostMapping("/list")
    @Operation(summary = "素材列表")
    public RESTResult<PageResultVO<Map<String, Object>>> list(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Integer page = body != null && body.get("page") instanceof Number n ? n.intValue() : 0;
        Integer rows = body != null && body.get("rows") instanceof Number n ? n.intValue() : 20;
        String materialType = body != null && body.get("materialType") instanceof String s ? s : null;
        Long projectId = body != null && body.get("projectId") instanceof Number n ? n.longValue() : null;
        PageResultVO<Map<String, Object>> result = materialLibraryService.search(page, rows, materialType, projectId, userId);
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除素材")
    public RESTResult<Void> delete(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long id = body != null && body.get("id") instanceof Number n ? n.longValue() : null;
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        materialLibraryService.delete(id, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
