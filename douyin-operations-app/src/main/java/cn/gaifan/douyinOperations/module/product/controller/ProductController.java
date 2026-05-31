package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;
import cn.gaifan.douyinOperations.contract.product.ProductCatalogProvider;
import cn.gaifan.douyinOperations.contract.auth.EntitlementProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 产品控制台 — 产品目录 + 授权检查
 * 参考 gaifan-ops ProductController
 */
@RestController("appProductController")
@RequestMapping("/api/v1/product")
@Tag(name = "产品控制台 / Product Console", description = "产品目录、授权检查、功能管理")
public class ProductController {

    @Resource
    private ProductCatalogProvider productCatalog;

    @Resource
    private EntitlementProvider entitlementProvider;

    @PostMapping("/list")
    @Operation(summary = "产品列表")
    public RESTResult<List<Map<String, Object>>> list() {
        IdentityContext ctx = RequestIdentityHolder.current();
        List<Map<String, Object>> products = productCatalog.listProducts().stream()
                .map(p -> Map.<String, Object>of(
                        "code", p.code(),
                        "name", p.name(),
                        "stage", p.stage(),
                        "enabled", p.enabled(),
                        "featureCount", p.features().size(),
                        "features", p.features().stream()
                                .map(f -> Map.<String, Object>of(
                                        "code", f.code(),
                                        "name", f.name(),
                                        "quotaUnit", f.quotaUnit(),
                                        "monthlyLimit", f.monthlyLimit()
                                )).collect(Collectors.toList())
                )).collect(Collectors.toList());

        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(products);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/check")
    @Operation(summary = "授权检查")
    public RESTResult<EntitlementDecision> check(@RequestBody Map<String, String> body) {
        IdentityContext ctx = RequestIdentityHolder.current();
        String productCode = body.getOrDefault("productCode", "douyin-ops");
        String featureCode = body.getOrDefault("featureCode", "");

        EntitlementDecision decision = entitlementProvider.check(
                ctx.tenantId(), ctx.userId(), productCode, featureCode);

        RESTResult<EntitlementDecision> r = RESTResult.getSuccess(decision);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/features")
    @Operation(summary = "产品功能列表")
    public RESTResult<List<Map<String, Object>>> features(@RequestBody Map<String, String> body) {
        String productCode = body.getOrDefault("productCode", "douyin-ops");
        var product = productCatalog.findProduct(productCode);
        if (product == null) {
            return RESTResult.error(404, "产品不存在: " + productCode);
        }
        List<Map<String, Object>> features = product.features().stream()
                .map(f -> Map.<String, Object>of(
                        "code", f.code(),
                        "name", f.name(),
                        "quotaUnit", f.quotaUnit(),
                        "monthlyLimit", f.monthlyLimit()
                )).collect(Collectors.toList());

        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(features);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
