package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptVersionService;
import cn.gaifan.douyinOperations.module.product.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 商品话术版本管理 Controller
 * API 路径：/api/v1/product/script-version/
 *
 * 功能：
 * - 话术版本查询、创建、更新、删除
 * - 版本搜索、推荐、引用
 * - 使用统计
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@RestController
@RequestMapping("/api/v1/product/script-version")
@Tag(name = "商品话术版本管理", description = "支持版本管理、搜索、推荐、快照引用")
public class ProductScriptVersionController {

    @Resource
    private ProductScriptVersionService versionService;

    /**
     * 保存或更新话术版本
     * POST /api/v1/product/script-version/save
     */
    @PostMapping("/save")
    @Operation(summary = "保存话术版本", description = "创建新版本或更新现有版本")
    public RESTResult<ProductScriptVersionVO> save(
            @Valid @RequestBody ProductScriptVersionSaveVO vo,
            @CurrentUserId Long userId) {
        ProductScriptVersionVO result = versionService.save(vo, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 分页查询话术版本
     * POST /api/v1/product/script-version/list
     */
    @PostMapping("/list")
    @Operation(summary = "查询话术版本列表", description = "支持分页、排序")
    public RESTResult<PageResultVO<ProductScriptVersionVO>> list(
            @Valid @RequestBody ProductScriptVersionSearchVO vo,
            @CurrentUserId Long userId) {
        PageResultVO<ProductScriptVersionVO> result = versionService.list(vo, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 搜索话术版本
     * POST /api/v1/product/script-version/search
     */
    @PostMapping("/search")
    @Operation(summary = "搜索话术版本", description = "支持关键词、风格、最小评分等条件")
    public RESTResult<PageResultVO<ProductScriptVersionVO>> search(
            @Parameter(name = "keyword", description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(name = "style", description = "话术风格") @RequestParam(required = false) String style,
            @Parameter(name = "minScore", description = "最小效果评分") @RequestParam(required = false) Double minScore,
            @Parameter(name = "page", description = "页码", required = true) @RequestParam Integer page,
            @Parameter(name = "rows", description = "每页数量", required = true) @RequestParam Integer rows,
            @CurrentUserId Long userId) {
        PageResultVO<ProductScriptVersionVO> result = versionService.search(keyword, style, minScore, page, rows, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 获取版本详情
     * POST /api/v1/product/script-version/detail/{id}
     */
    @PostMapping("/detail/{id}")
    @Operation(summary = "获取版本详情", description = "返回指定版本的详细信息")
    public RESTResult<ProductScriptVersionVO> getDetail(
            @Parameter(name = "id", description = "版本 ID") @PathVariable Long id,
            @CurrentUserId Long userId) {
        ProductScriptVersionVO result = versionService.getDetail(id, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 获取推荐版本
     * POST /api/v1/product/script-version/recommend
     */
    @PostMapping("/recommend")
    @Operation(summary = "获取推荐版本", description = "基于效果评分和使用频率推荐最优版本")
    public RESTResult<ProductScriptRecommendVO> recommend(
            @Parameter(name = "productId", description = "产品 ID", required = true) @RequestParam Long productId,
            @Parameter(name = "topN", description = "返回前 N 个，默认 5") @RequestParam(defaultValue = "5") Integer topN,
            @CurrentUserId Long userId) {
        ProductScriptRecommendVO result = versionService.recommend(productId, topN, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 更新话术效果评分
     * POST /api/v1/product/script-version/update-effectiveness
     */
    @PostMapping("/update-effectiveness")
    @Operation(summary = "更新效果评分", description = "更新话术版本的效果评分和转化率")
    public RESTResult<ProductScriptVersionVO> updateEffectiveness(
            @Valid @RequestBody ProductScriptVersionSaveVO vo,
            @CurrentUserId Long userId) {
        Double effectiveness = vo.getEffectivenessScore() != null ? Double.valueOf(vo.getEffectivenessScore().doubleValue()) : null;
        Double conversion = vo.getConversionRate() != null ? Double.valueOf(vo.getConversionRate().doubleValue()) : null;
        ProductScriptVersionVO result = versionService.updateEffectiveness(vo.getId(), effectiveness, conversion, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 从话术库引用到直播场次
     * POST /api/v1/product/script-version/apply-from-library
     */
    @PostMapping("/apply-from-library")
    @Operation(summary = "引用到直播场次", description = "将话术库版本引用到直播场次，自动创建快照")
    public RESTResult<ProductScriptSnapshotVO> applyFromLibrary(
            @Parameter(name = "liveSessionId", description = "直播场次 ID", required = true) @RequestParam Long liveSessionId,
            @Parameter(name = "productScriptVersionId", description = "话术版本 ID", required = true) @RequestParam Long productScriptVersionId,
            @CurrentUserId Long userId) {
        ProductScriptSnapshotVO result = versionService.applyFromLibrary(liveSessionId, productScriptVersionId, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 删除话术版本
     * POST /api/v1/product/script-version/delete/{id}
     */
    @PostMapping("/delete/{id}")
    @Operation(summary = "删除版本", description = "逻辑删除指定的话术版本")
    public RESTResult<Boolean> delete(
            @Parameter(name = "id", description = "版本 ID") @PathVariable Long id,
            @CurrentUserId Long userId) {
        boolean result = versionService.delete(id, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 更新版本状态
     * POST /api/v1/product/script-version/update-status
     */
    @PostMapping("/update-status")
    @Operation(summary = "更新版本状态", description = "启用或禁用话术版本")
    public RESTResult<ProductScriptVersionVO> updateStatus(
            @Parameter(name = "id", description = "版本 ID", required = true) @RequestParam Long id,
            @Parameter(name = "isActive", description = "是否启用", required = true) @RequestParam Boolean isActive,
            @CurrentUserId Long userId) {
        ProductScriptVersionVO result = versionService.updateStatus(id, isActive, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 获取产品的所有版本
     * POST /api/v1/product/script-version/list-by-product
     */
    @PostMapping("/list-by-product")
    @Operation(summary = "获取产品的所有版本", description = "返回指定产品的所有话术版本列表")
    public RESTResult<List<ProductScriptVersionVO>> listByProductId(
            @Parameter(name = "productId", description = "产品 ID", required = true) @RequestParam Long productId,
            @CurrentUserId Long userId) {
        List<ProductScriptVersionVO> result = versionService.listByProductId(productId, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 获取最优版本
     * POST /api/v1/product/script-version/best
     */
    @PostMapping("/best")
    @Operation(summary = "获取最优版本", description = "返回产品的最优话术版本（基于推荐分数）")
    public RESTResult<ProductScriptVersionVO> findBestVersion(
            @Parameter(name = "productId", description = "产品 ID", required = true) @RequestParam Long productId,
            @CurrentUserId Long userId) {
        ProductScriptVersionVO result = versionService.findBestVersion(productId, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 增加使用次数
     * POST /api/v1/product/script-version/increase-usage
     */
    @PostMapping("/increase-usage")
    @Operation(summary = "增加使用次数", description = "版本被使用时调用，自动更新使用统计")
    public RESTResult<ProductScriptVersionVO> increaseUsageCount(
            @Parameter(name = "id", description = "版本 ID", required = true) @RequestParam Long id,
            @CurrentUserId Long userId) {
        ProductScriptVersionVO result = versionService.increaseUsageCount(id, userId);
        return RESTResult.getSuccess(result);
    }

    /**
     * 为商品库主话术创建或复用优化链路所需的 ProductScriptVersion 镜像。
     * POST /api/v1/product/script-version/ensure-optimization-version
     */
    @PostMapping("/ensure-optimization-version")
    @Operation(summary = "确保优化版本镜像", description = "将 dy_product_script 主话术映射为可参与 analyze/suggestions/regenerate 的 product_script_version")
    public RESTResult<EnsureOptimizationVersionResultVO> ensureOptimizationVersion(
            @Valid @RequestBody EnsureOptimizationVersionVO vo,
            @CurrentUserId Long userId) {
        EnsureOptimizationVersionResultVO result = versionService.ensureOptimizationVersion(vo, userId);
        return RESTResult.getSuccess(result);
    }

}
