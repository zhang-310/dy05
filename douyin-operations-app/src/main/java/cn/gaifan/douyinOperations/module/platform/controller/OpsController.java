package cn.gaifan.douyinOperations.module.platform.controller;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.port.MetricPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 运维监控控制器
 */
@RestController
@RequestMapping("/api/v1/ops")
@Tag(name = "运维监控 / Ops", description = "系统指标、健康检查、导出任务")
public class OpsController {

    @Resource
    private MetricPort metricPort;

    @PostMapping("/metrics/record")
    @Operation(summary = "记录指标")
    public RESTResult<Void> record(@RequestBody Map<String, Object> body) {
        String name = (String) body.getOrDefault("name", "unknown");
        double value = body.get("value") instanceof Number n ? n.doubleValue() : 0;
        String dim = (String) body.getOrDefault("dimension", "");
        metricPort.record(name, value, dim);
        return RESTResult.success(null);
    }

    @PostMapping("/metrics/query")
    @Operation(summary = "查询指标")
    public RESTResult<Object> query(@RequestBody Map<String, Object> body) {
        String name = (String) body.getOrDefault("name", "gmv");
        int minutes = body.get("minutes") instanceof Number n ? n.intValue() : 60;
        var results = metricPort.query(name, "default", minutes);
        RESTResult<Object> r = RESTResult.getSuccess(Map.of("metrics", results));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
