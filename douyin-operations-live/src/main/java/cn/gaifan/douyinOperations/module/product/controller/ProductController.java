package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.product.service.PaipingImportService;
import cn.gaifan.douyinOperations.module.product.service.ProductLinkExtractService;
import cn.gaifan.douyinOperations.module.product.service.impl.ProductServiceImpl;
import cn.gaifan.douyinOperations.module.product.service.impl.SalesHistoryServiceImpl;
import cn.gaifan.douyinOperations.module.product.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/product")
@Tag(name = "商品管理 / Product", description = "商品库管理（需登录）")
public class ProductController {

    @Resource
    private ProductServiceImpl productService;
    @Resource
    private SalesHistoryServiceImpl salesHistoryService;
    @Resource
    private PaipingImportService paipingImportService;
    @Resource
    private ProductLinkExtractService productLinkExtractService;
    @Resource
    private DataScopeResolver dataScopeService;

    @PostMapping("/search")
    @Operation(summary = "分页搜索商品")
    public RESTResult<PageResultVO<ProductVO>> search(HttpServletRequest request,
            @RequestBody(required = false) ProductSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new ProductSearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) vo.setUserIds(visibleIds);
        RESTResult<PageResultVO<ProductVO>> r = RESTResult.getSuccess(productService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取商品详情")
    public RESTResult<ProductVO> get(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<ProductVO> r = RESTResult.getSuccess(productService.getById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/infer-product-type")
    @Operation(summary = "根据产品属性自动推断产品分类（用于直播选品话术生成）")
    public RESTResult<String> inferProductType(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long productId = parseLong(body, "productId");
        if (productId == null || productId <= 0) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "商品 ID 无效");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String productType = productService.inferProductType(productId);
        RESTResult<String> r = RESTResult.getSuccess(productType);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/trigger-extract")
    @Operation(summary = "手动触发异步提取（从商品链接获取关键信息、AI 提炼卖点）")
    public RESTResult<Void> triggerExtract(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long productId = parseLong(body, "productId");
        if (productId == null || productId <= 0) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "商品 ID 无效");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        productLinkExtractService.extractAndSaveAsync(productId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        r.setMessage("已提交异步提取，请稍后刷新查看");
        return r;
    }

    @PostMapping("/extract-from-link")
    @Operation(summary = "从商品链接提取信息（标题、图片、描述、AI 提炼卖点）")
    public RESTResult<java.util.Map<String, String>> extractFromLink(HttpServletRequest request,
            @RequestBody java.util.Map<String, String> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String productLink = body != null ? body.get("productLink") : null;
        if (productLink == null || productLink.isBlank()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "请提供商品链接");
        }
        try {
            java.util.Map<String, String> result = productLinkExtractService.extractFromLink(productLink);
            RESTResult<java.util.Map<String, String>> r = RESTResult.getSuccess(result);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "提取失败: " + e.getMessage());
        }
    }

    @PostMapping("/import-paiping")
    @Operation(summary = "导入排品表 Excel（paiping 格式）")
    public RESTResult<java.util.Map<String, Object>> importPaiping(HttpServletRequest request,
                                                                   @RequestParam("file") MultipartFile file) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (file == null || file.isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "请上传文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls"))) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "仅支持 .xlsx / .xls 格式");
        }
        try {
            java.util.List<ProductSaveVO> list = paipingImportService.parseFromExcel(file.getInputStream(), userId);
            int success = 0;
            int fail = 0;
            for (ProductSaveVO vo : list) {
                vo.setUserId(userId);
                try {
                    productService.save(vo);
                    success++;
                } catch (Exception e) {
                    fail++;
                }
            }
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("total", list.size());
            result.put("success", success);
            result.put("fail", fail);
            RESTResult<java.util.Map<String, Object>> r = RESTResult.getSuccess(result);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    @Operation(summary = "新增/更新商品")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody ProductSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setUserId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(productService.save(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除商品")
    public RESTResult<Void> delete(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        productService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除商品")
    public RESTResult<Void> batchDelete(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        @SuppressWarnings("unchecked")
        List<Long> ids = body != null && body.get("ids") != null
            ? ((List<?>) body.get("ids")).stream()
                .map(o -> o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString()))
                .collect(java.util.stream.Collectors.toList())
            : null;
        if (ids == null || ids.isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 ids");
        }
        productService.batchDelete(ids);
        RESTResult<Void> r = RESTResult.success("批量删除成功", null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update-inventory")
    @Operation(summary = "更新库存（delta 可为负数）")
    public RESTResult<Void> updateInventory(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        Long quantity = body != null && body.get("quantity") != null ? ((Number) body.get("quantity")).longValue() : null;
        if (id == null || quantity == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id 或 quantity");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        productService.updateInventory(id, quantity);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/publish")
    @Operation(summary = "上架商品")
    public RESTResult<Void> publish(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        productService.publish(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/unpublish")
    @Operation(summary = "下架商品")
    public RESTResult<Void> unpublish(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        productService.unpublish(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/set-featured")
    @Operation(summary = "设置推荐标记")
    public RESTResult<Void> setFeatured(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        Integer featured = body != null && body.get("featured") != null ? ((Number) body.get("featured")).intValue() : null;
        if (id == null || featured == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id 或 featured");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        productService.setFeatured(id, featured);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 销售历史 ====================

    @PostMapping("/sales-history/search")
    @Operation(summary = "分页搜索销售历史")
    public RESTResult<PageResultVO<SalesHistoryVO>> searchSales(HttpServletRequest request,
            @RequestBody(required = false) SalesHistorySearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SalesHistorySearchVO();
        String roleCode = AuthTokenFilter.getRoleCode(request);
        RESTResult<PageResultVO<SalesHistoryVO>> r = RESTResult.getSuccess(
                salesHistoryService.search(vo, userId, roleCode));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/sales-history/get")
    @Operation(summary = "获取销售记录详情")
    public RESTResult<SalesHistoryVO> getSales(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        RESTResult<SalesHistoryVO> r = RESTResult.getSuccess(salesHistoryService.getById(id, userId, roleCode));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseLong(java.util.Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }

    @PostMapping("/sales-history/save")
    @Operation(summary = "新增销售记录")
    public RESTResult<Long> saveSales(HttpServletRequest request, @Valid @RequestBody SalesHistorySaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        RESTResult<Long> r = RESTResult.addSuccess(salesHistoryService.save(vo, userId, roleCode));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/sales-history/total-sales-amount")
    @Operation(summary = "查询累计销售额")
    public RESTResult<BigDecimal> totalSalesAmount(HttpServletRequest request,
                                                    @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body != null ? body.get("productId") : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        RESTResult<BigDecimal> r = RESTResult.getSuccess(salesHistoryService.totalSalesAmount(productId, userId, roleCode));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/sales-history/total-sales-quantity")
    @Operation(summary = "查询累计销售量")
    public RESTResult<Long> totalSalesQuantity(HttpServletRequest request,
                                                @RequestBody(required = false) java.util.Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long productId = body != null ? body.get("productId") : null;
        if (productId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 productId");
        String roleCode = AuthTokenFilter.getRoleCode(request);
        RESTResult<Long> r = RESTResult.getSuccess(salesHistoryService.totalSalesQuantity(productId, userId, roleCode));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
