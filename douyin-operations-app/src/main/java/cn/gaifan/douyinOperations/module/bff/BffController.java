package cn.gaifan.douyinOperations.module.bff;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.bff.BffDashboardVO;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * BFF 聚合控制器
 *
 * 为前端仪表盘提供多域聚合数据。
 * 微服务模式下，此控制器仅部署在 platform 角色中。
 */
@RestController
@RequestMapping("/api/v1/bff")
@Tag(name = "BFF / 前端聚合", description = "前端仪表盘和后端聚合接口")
public class BffController {

    @Resource
    private BffService bffService;

    @PostMapping("/dashboard")
    @Operation(summary = "概览仪表盘聚合")
    public RESTResult<BffDashboardVO> dashboard() {
        IdentityContext ctx = RequestIdentityHolder.current();
        BffDashboardVO data = bffService.buildDashboard(ctx);
        RESTResult<BffDashboardVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/identity")
    @Operation(summary = "当前身份信息")
    public RESTResult<Map<String, Object>> identity() {
        IdentityContext ctx = RequestIdentityHolder.current();
        Map<String, Object> info = Map.of(
                "authenticated", ctx.authenticated(),
                "tenantId", ctx.tenantId(),
                "userId", ctx.userId() != null ? ctx.userId() : null,
                "roleCode", ctx.roleCode() != null ? ctx.roleCode() : null,
                "channel", ctx.channel(),
                "source", ctx.source()
        );
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(info);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
