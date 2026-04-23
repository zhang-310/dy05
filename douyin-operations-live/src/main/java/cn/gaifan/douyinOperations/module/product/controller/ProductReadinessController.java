package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.product.service.ProductReadinessService;
import cn.gaifan.douyinOperations.module.product.vo.ProductReadinessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商品上播准备度检测 Controller
 */
@RestController
@RequestMapping("/api/v1/product")
@Tag(name = "商品上播准备度 / Product Readiness", description = "商品上播准备度检测（需登录）")
public class ProductReadinessController {

    @Resource
    private ProductReadinessService productReadinessService;

    @PostMapping("/readiness")
    @Operation(summary = "检测商品上播准备度 / Check Product Readiness")
    public RESTResult<ProductReadinessVO> checkReadiness(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        Long productId = body != null && body.get("productId") != null
            ? ((Number) body.get("productId")).longValue()
            : null;
        if (productId == null) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");
        }

        try {
            ProductReadinessVO result = productReadinessService.checkReadiness(productId, userId);
            RESTResult<ProductReadinessVO> r = RESTResult.getSuccess(result);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "检测失败: " + e.getMessage());
        }
    }
}
