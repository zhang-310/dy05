package cn.gaifan.douyinOperations.module.governance.controller;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.module.ai.gateway.AiGatewayService;
import cn.gaifan.douyinOperations.module.platform.service.GovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 治理控制台 — 审计、用量、成本仪表盘
 * 参考 gaifan-ops GovernanceController
 */
@RestController
@RequestMapping("/api/v1/governance")
@Tag(name = "治理控制台 / Governance Console", description = "审计日志、AI 用量、成本仪表盘")
public class GovernanceController {

    @Resource
    private GovernanceService governanceService;

    @Resource
    private AiGatewayService aiGateway;

    @PostMapping("/overview")
    @Operation(summary = "治理总览")
    public RESTResult<Map<String, Object>> overview() {
        IdentityContext ctx = RequestIdentityHolder.current();
        Map<String, Object> data = Map.of(
                "tenantId", ctx.tenantId(),
                "userId", ctx.userId(),
                "aiUsageByFeature", aiGateway.getUsageStats(ctx.tenantId())
        );
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/audit/record")
    @Operation(summary = "记录审计事件")
    public RESTResult<Void> recordEvent(@RequestBody Map<String, Object> body) {
        String action = String.valueOf(body.getOrDefault("action", "unknown"));
        String resource = String.valueOf(body.getOrDefault("resource", "unknown"));
        String detail = String.valueOf(body.getOrDefault("detail", ""));
        boolean success = Boolean.TRUE.equals(body.get("success"));

        governanceService.recordEvent(action, resource, detail, success);
        return RESTResult.success(null);
    }
}
