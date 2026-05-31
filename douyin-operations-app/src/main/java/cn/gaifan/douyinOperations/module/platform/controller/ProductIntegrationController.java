package cn.gaifan.douyinOperations.module.platform.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.contract.product.*;
import cn.gaifan.douyinOperations.module.platform.product.ProductIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/product-integrations")
@Tag(name = "产品互调 / Product Integrations")
public class ProductIntegrationController {

    private final ProductIntegrationService productIntegrationService;

    public ProductIntegrationController(ProductIntegrationService productIntegrationService) {
        this.productIntegrationService = productIntegrationService;
    }

    @GetMapping("/rules")
    @Operation(summary = "互调规则列表")
    public RESTResult<List<ProductIntegrationRuleSummary>> rules() {
        return RESTResult.success(productIntegrationService.listRules());
    }

    @PostMapping("/check")
    @Operation(summary = "互调预检")
    public RESTResult<ProductIntegrationDecision> check(@RequestBody ProductIntegrationCheckRequest request) {
        return RESTResult.success(productIntegrationService.check(request));
    }

    @PostMapping("/invoke")
    @Operation(summary = "执行互调记账")
    public ResponseEntity<RESTResult<ProductIntegrationInvocationResult>> invoke(
            @RequestBody ProductIntegrationInvocationRequest request) {
        ProductIntegrationInvocationResult result = productIntegrationService.invoke(request);
        HttpStatus status = result.allowed() ? HttpStatus.OK : HttpStatus.PAYMENT_REQUIRED;
        return ResponseEntity.status(status).body(RESTResult.success(result));
    }
}
