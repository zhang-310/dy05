package cn.gaifan.douyinOperations.module.log.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.log.service.OperationLogService;
import cn.gaifan.douyinOperations.module.log.service.SystemLogService;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogVO;
import cn.gaifan.douyinOperations.module.log.vo.SystemLogSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.SystemLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 操作日志、系统日志分页查询（需登录，建议仅管理员可访问）
 * Operation Log & System Log Pagination Query (Authentication required, Admin recommended)
 */
@RestController
@RequestMapping("/api/v1/log")
@Tag(name = "日志管理 / Log Management", description = "操作日志和系统日志的查询和导出（需登录）")
public class LogController {

    @Resource
    private OperationLogService operationLogService;
    @Resource
    private SystemLogService systemLogService;

    @PostMapping("/operation/page")
    @Operation(
            summary = "查询操作日志 / Search Operation Logs",
            description = "分页查询操作日志（需登录） / Search and paginate operation logs (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<OperationLogVO>> operationPage(HttpServletRequest request,
                                                                  @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                          description = "查询条件 / Search criteria",
                                                                          required = false
                                                                  )
                                                                  @RequestBody(required = false) OperationLogSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        PageResultVO<OperationLogVO> data = operationLogService.search(vo != null ? vo : new OperationLogSearchVO());
        RESTResult<PageResultVO<OperationLogVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/system/page")
    @Operation(
            summary = "查询系统日志 / Search System Logs",
            description = "分页查询系统日志（需登录） / Search and paginate system logs (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<SystemLogVO>> systemPage(HttpServletRequest request,
                                                            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                    description = "查询条件 / Search criteria",
                                                                    required = false
                                                            )
                                                            @RequestBody(required = false) SystemLogSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        PageResultVO<SystemLogVO> data = systemLogService.search(vo != null ? vo : new SystemLogSearchVO());
        RESTResult<PageResultVO<SystemLogVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static final int EXPORT_MAX_ROWS = 2000;

    @PostMapping("/operation/export")
    @Operation(
            summary = "导出操作日志 CSV / Export Operation Logs",
            description = "导出操作日志为 CSV 文件（仅登录用户，最多 2000 条）/ Export operation logs as CSV file (authenticated users only, max 2000 rows)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导出成功 / Export successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public void operationExport(HttpServletRequest request, HttpServletResponse response,
                                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                        description = "查询条件 / Search criteria",
                                        required = false
                                )
                                @RequestBody(required = false) OperationLogSearchVO vo) throws IOException {
        if (AuthTokenFilter.getUserId(request) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (vo == null) vo = new OperationLogSearchVO();
        if (vo.getRows() == null || vo.getRows() <= 0) vo.setRows(EXPORT_MAX_ROWS);
        if (vo.getRows() > EXPORT_MAX_ROWS) vo.setRows(EXPORT_MAX_ROWS);
        PageResultVO<OperationLogVO> page = operationLogService.search(vo);
        List<OperationLogVO> list = page.getList();
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=operation-log-" + System.currentTimeMillis() + ".csv");
        response.addHeader("Content-Disposition", "attachment; filename*=UTF-8''operation-log-" + System.currentTimeMillis() + ".csv");
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            w.write("\uFEFF"); // BOM for Excel
            w.println("ID,traceId,userId,username,module,action,requestUri,requestMethod,ip,durationMs,status,createTime");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (OperationLogVO o : list) {
                w.println(escapeCsv(o.getId()) + "," + escapeCsv(o.getTraceId()) + "," + escapeCsv(o.getUserId()) + ","
                        + escapeCsv(o.getUsername()) + "," + escapeCsv(o.getModule()) + "," + escapeCsv(o.getAction()) + ","
                        + escapeCsv(o.getRequestUri()) + "," + escapeCsv(o.getRequestMethod()) + "," + escapeCsv(o.getIp()) + ","
                        + escapeCsv(o.getDurationMs()) + "," + escapeCsv(o.getStatus()) + ","
                        + (o.getCreateTime() != null ? escapeCsv(sdf.format(o.getCreateTime())) : ""));
            }
        }
    }

    @PostMapping("/system/export")
    @Operation(
            summary = "导出系统日志 CSV / Export System Logs",
            description = "导出系统日志为 CSV 文件（仅登录用户，最多 2000 条）/ Export system logs as CSV file (authenticated users only, max 2000 rows)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "导出成功 / Export successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public void systemExport(HttpServletRequest request, HttpServletResponse response,
                             @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                     description = "查询条件 / Search criteria",
                                     required = false
                             )
                             @RequestBody(required = false) SystemLogSearchVO vo) throws IOException {
        if (AuthTokenFilter.getUserId(request) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        if (vo == null) vo = new SystemLogSearchVO();
        if (vo.getRows() == null || vo.getRows() <= 0) vo.setRows(EXPORT_MAX_ROWS);
        if (vo.getRows() > EXPORT_MAX_ROWS) vo.setRows(EXPORT_MAX_ROWS);
        PageResultVO<SystemLogVO> page = systemLogService.search(vo);
        List<SystemLogVO> list = page.getList();
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=system-log-" + System.currentTimeMillis() + ".csv");
        response.addHeader("Content-Disposition", "attachment; filename*=UTF-8''system-log-" + System.currentTimeMillis() + ".csv");
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            w.write("\uFEFF");
            w.println("ID,module,eventType,summary,status,createTime");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (SystemLogVO o : list) {
                w.println(escapeCsv(o.getId()) + "," + escapeCsv(o.getModule()) + "," + escapeCsv(o.getEventType()) + ","
                        + escapeCsv(o.getSummary()) + "," + escapeCsv(o.getStatus()) + ","
                        + (o.getCreateTime() != null ? escapeCsv(sdf.format(o.getCreateTime())) : ""));
            }
        }
    }

    private static String escapeCsv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
