package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统性能监控 API（前后端对齐桩）
 * 提供 /system/performance/* 桩接口，避免前端 performance.ts 调用 404。
 */
@RestController
@RequestMapping("/api/v1/system/performance")
public class SystemPerformanceController {

    @PostMapping("/metrics/current")
    public RESTResult<Map<String, Object>> getMetricsCurrent(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("timestamp", System.currentTimeMillis(), "cpu", 0, "memory", 0));
    }

    @PostMapping("/metrics/search")
    public RESTResult<PageResultVO<Map<String, Object>>> getMetricsSearch(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(PageResultVO.of(0L, List.of(), 0, 30));
    }

    @PostMapping("/api/timeseries")
    public RESTResult<List<Map<String, Object>>> getApiTimeseries(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/query/analysis")
    public RESTResult<Map<String, Object>> getQueryAnalysis(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of());
    }

    @PostMapping("/query/slow")
    public RESTResult<List<Map<String, Object>>> getSlowQueries(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/query/n-plus-one")
    public RESTResult<List<Map<String, Object>>> getNPlusOneQueries(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/index/suggestions")
    public RESTResult<List<Map<String, Object>>> getIndexSuggestions(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/cache/statistics")
    public RESTResult<Map<String, Object>> getCacheStatistics(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("hitCount", 0, "missCount", 0));
    }

    @PostMapping("/cache/hot-keys")
    public RESTResult<List<Map<String, Object>>> getCacheHotKeys(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/cache/trend")
    public RESTResult<List<Map<String, Object>>> getCacheTrend(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(List.of());
    }

    @PostMapping("/cache/clear")
    public RESTResult<Map<String, Object>> clearCache(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("clearedCount", 0));
    }

    @PostMapping("/cache/rebuild")
    public RESTResult<Map<String, Object>> rebuildCache(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("jobId", ""));
    }

    @PostMapping(value = "/export", produces = "application/octet-stream")
    public org.springframework.http.ResponseEntity<byte[]> exportPerformance(@RequestBody(required = false) Map<String, Object> body) {
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=performance.csv")
                .body(new byte[0]);
    }

    @PostMapping("/benchmark")
    public RESTResult<Map<String, Object>> runBenchmark(@RequestBody(required = false) Map<String, Object> body) {
        return RESTResult.success(Map.of("status", "idle"));
    }
}
