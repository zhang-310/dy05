package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.product.service.StyleRecommendationMLService;
import cn.gaifan.douyinOperations.module.product.vo.ModelMetricsVO;
import cn.gaifan.douyinOperations.module.product.vo.StyleRecommendationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 风格智能推荐 Controller
 *
 * @author Claude Code
 * @since 2026-04-05
 */
@RestController
@RequestMapping("/api/v1/product/style-recommendation")
@Tag(name = "风格智能推荐", description = "基于机器学习的风格推荐API")
public class StyleRecommendationController {

    @Resource
    private StyleRecommendationMLService mlService;

    /**
     * 混合推荐风格（ML模型 + 规则引擎）
     */
    @PostMapping("/recommend-hybrid")
    @Operation(summary = "混合推荐风格", description = "结合ML模型和规则引擎推荐最佳风格")
    public RESTResult<List<StyleRecommendationVO>> recommendHybrid(
            HttpServletRequest request,
            @Valid @RequestBody RecommendRequest req) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        List<StyleRecommendationVO> result = mlService.recommendHybrid(
                req.getProductId(),
                userId,
                req.getTopK() != null ? req.getTopK() : 5
        );

        RESTResult<List<StyleRecommendationVO>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 仅使用ML模型推荐
     */
    @PostMapping("/recommend-ml")
    @Operation(summary = "ML模型推荐", description = "仅使用机器学习模型推荐风格")
    public RESTResult<List<StyleRecommendationVO>> recommendML(
            HttpServletRequest request,
            @Valid @RequestBody RecommendRequest req) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        List<StyleRecommendationVO> result = mlService.recommendWithML(
                req.getProductId(),
                userId,
                req.getTopK() != null ? req.getTopK() : 5
        );

        RESTResult<List<StyleRecommendationVO>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 训练推荐模型
     */
    @PostMapping("/train")
    @Operation(summary = "训练推荐模型", description = "使用当前用户的历史数据训练ML模型")
    public RESTResult<TrainResponse> trainModel(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        try {
            Long modelId = mlService.trainModel(userId);
            TrainResponse response = new TrainResponse();
            response.setModelId(modelId);
            response.setMessage("模型训练成功");

            RESTResult<TrainResponse> r = RESTResult.getSuccess(response);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "模型训练失败: " + e.getMessage());
        }
    }

    /**
     * 异步训练推荐模型
     */
    @PostMapping("/train-async")
    @Operation(summary = "异步训练推荐模型", description = "后台异步训练ML模型，不阻塞请求")
    public RESTResult<TrainResponse> trainModelAsync(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        try {
            Long taskId = mlService.trainModelAsync(userId);
            TrainResponse response = new TrainResponse();
            response.setModelId(taskId);
            response.setMessage("模型训练任务已提交，请稍后查询结果");

            RESTResult<TrainResponse> r = RESTResult.getSuccess(response);
            r.setTraceId(MDC.get("traceId"));
            return r;
        } catch (Exception e) {
            return RESTResult.error(ErrorCode.SYSTEM_ERROR, "提交训练任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取模型性能指标
     */
    @GetMapping("/metrics")
    @Operation(summary = "获取模型性能指标", description = "查询当前激活模型的性能指标")
    public RESTResult<ModelMetricsVO> getMetrics(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        ModelMetricsVO metrics = mlService.getModelMetrics(userId);

        RESTResult<ModelMetricsVO> r = RESTResult.getSuccess(metrics);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 记录推荐反馈
     */
    @PostMapping("/record-feedback")
    @Operation(summary = "记录推荐反馈", description = "记录用户对推荐结果的选择，用于模型优化")
    public RESTResult<Void> recordFeedback(
            HttpServletRequest request,
            @Valid @RequestBody FeedbackRequest req) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        mlService.recordFeedback(
                req.getProductId(),
                req.getRecommendedStyles(),
                req.getSelectedStyles(),
                userId
        );

        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 查找相似商品
     */
    @PostMapping("/similar-products")
    @Operation(summary = "查找相似商品", description = "基于特征相似度查找相似商品")
    public RESTResult<List<StyleRecommendationVO.SimilarProduct>> findSimilarProducts(
            HttpServletRequest request,
            @Valid @RequestBody SimilarProductRequest req) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        List<StyleRecommendationVO.SimilarProduct> result = mlService.findSimilarProducts(
                req.getProductId(),
                userId,
                req.getTopK() != null ? req.getTopK() : 5
        );

        RESTResult<List<StyleRecommendationVO.SimilarProduct>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 计算商品特征向量
     */
    @PostMapping("/compute-features")
    @Operation(summary = "计算商品特征向量", description = "提取商品特征并缓存，用于相似度计算")
    public RESTResult<ComputeFeaturesResponse> computeFeatures(
            HttpServletRequest request,
            @Valid @RequestBody ComputeFeaturesRequest req) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String featureVector = mlService.computeProductFeatures(req.getProductId(), userId);

        ComputeFeaturesResponse response = new ComputeFeaturesResponse();
        response.setProductId(req.getProductId());
        response.setFeatureVector(featureVector);
        response.setMessage("特征计算成功");

        RESTResult<ComputeFeaturesResponse> r = RESTResult.getSuccess(response);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ================ 请求/响应 VO ================

    @Data
    public static class RecommendRequest {
        private Long productId;
        private Integer topK;
    }

    @Data
    public static class TrainResponse {
        private Long modelId;
        private String message;
    }

    @Data
    public static class FeedbackRequest {
        private Long productId;
        private List<String> recommendedStyles;
        private List<String> selectedStyles;
    }

    @Data
    public static class SimilarProductRequest {
        private Long productId;
        private Integer topK;
    }

    @Data
    public static class ComputeFeaturesRequest {
        private Long productId;
    }

    @Data
    public static class ComputeFeaturesResponse {
        private Long productId;
        private String featureVector;
        private String message;
    }
}
