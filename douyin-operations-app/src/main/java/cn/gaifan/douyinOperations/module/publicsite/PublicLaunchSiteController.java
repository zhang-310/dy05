package cn.gaifan.douyinOperations.module.publicsite;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.platform.service.ProductCatalogServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public-site")
@Tag(name = "官网 BFF / Public Site")
public class PublicLaunchSiteController {

    private final ProductCatalogServiceImpl productCatalog;

    public PublicLaunchSiteController(ProductCatalogServiceImpl productCatalog) {
        this.productCatalog = productCatalog;
    }

    @GetMapping("/overview")
    @Operation(summary = "官网售卖总览")
    public RESTResult<Map<String, Object>> overview(
            @RequestParam(name = "tenantId", required = false, defaultValue = "demo-tenant") String tenantId) {
        var products = productCatalog.listProducts().stream()
                .map(p -> Map.<String, Object>of(
                        "productCode", p.code(),
                        "productName", p.name(),
                        "stage", p.stage(),
                        "enabled", p.enabled()
                ))
                .toList();
        return RESTResult.success(Map.of(
                "tenantId", tenantId,
                "headline", "Gaifan Ops 多产品商业化平台",
                "products", products
        ));
    }

    @GetMapping("/products/{productCode}/landing")
    public RESTResult<Map<String, Object>> landing(
            @PathVariable String productCode,
            @RequestParam(name = "tenantId", required = false, defaultValue = "demo-tenant") String tenantId) {
        var p = productCatalog.findProduct(productCode);
        if (p == null) {
            return RESTResult.error(404, "产品不存在");
        }
        return RESTResult.success(Map.of(
                "tenantId", tenantId,
                "productCode", p.code(),
                "productName", p.name(),
                "cta", "立即购买",
                "features", p.features()
        ));
    }
}
