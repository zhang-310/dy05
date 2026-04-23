package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/script/admin/violation")
@Tag(name = "违规词管理（Admin）", description = "公共违规词管理，仅管理员可用")
public class ViolationWordAdminController {

    @Resource
    private ViolationWordService violationWordService;

    @PostMapping("/list")
    @Operation(summary = "违规词管理列表")
    public RESTResult<PageResultVO<ViolationWordVO>> list(HttpServletRequest request,
            @RequestBody(required = false) ViolationWordSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new ViolationWordSearchVO();
        RESTResult<PageResultVO<ViolationWordVO>> r = RESTResult.getSuccess(violationWordService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新增/编辑违规词")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ViolationWordSaveVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<Long> r = RESTResult.addSuccess(violationWordService.save(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除违规词")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        violationWordService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/active")
    @Operation(summary = "获取所有启用违规词")
    public RESTResult<List<ViolationWordVO>> active(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<ViolationWordVO>> r = RESTResult.getSuccess(violationWordService.listActive());
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入违规词（CSV）")
    public RESTResult<Map<String, Object>> importCsv(HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!"admin".equalsIgnoreCase(AuthTokenFilter.getRoleCode(request))) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可导入违规词");
        }
        if (file == null || file.isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "请选择 CSV 文件");
        }
        try {
            Map<String, Object> data = violationWordService.importFromCsv(file.getBytes());
            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.CSV_FORMAT_ERROR, "CSV 导入失败: " + e.getMessage());
        }
    }

    @PostMapping("/export")
    @Operation(summary = "导出违规词（CSV）")
    public ResponseEntity<byte[]> exportCsv(HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return ResponseEntity.status(401).build();
        }
        if (!"admin".equalsIgnoreCase(AuthTokenFilter.getRoleCode(request))) {
            return ResponseEntity.status(403).build();
        }
        byte[] csv = violationWordService.exportToCsv();
        String filename = "violation_words_" + System.currentTimeMillis() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
