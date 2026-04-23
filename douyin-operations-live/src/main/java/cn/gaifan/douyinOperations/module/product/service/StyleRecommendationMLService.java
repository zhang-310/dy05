package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.vo.ModelMetricsVO;
import cn.gaifan.douyinOperations.module.product.vo.StyleRecommendationVO;

import java.util.List;

/**
 * 智能推荐服务 - 机器学习增强版
 *
 * @author Claude Code
 * @since 2026-04-04
 */
public interface StyleRecommendationMLService {

    /**
     * 基于机器学习模型推荐风格
     *
     * @param productId 商品ID
     * @param userId    用户ID
     * @param topK      返回Top-K个推荐结果
     * @return 推荐结果列表（按置信度降序）
     */
    List<StyleRecommendationVO> recommendWithML(Long productId, Long userId, Integer topK);

    /**
     * 混合推荐：结合规则引擎和ML模型
     *
     * @param productId 商品ID
     * @param userId    用户ID
     * @param topK      返回Top-K个推荐结果
     * @return 推荐结果列表
     */
    List<StyleRecommendationVO> recommendHybrid(Long productId, Long userId, Integer topK);

    /**
     * 训练推荐模型（异步任务）
     *
     * @param userId 用户ID
     * @return 训练任务ID
     */
    Long trainModelAsync(Long userId);

    /**
     * 训练推荐模型（同步）
     *
     * @param userId 用户ID
     * @return 模型ID
     */
    Long trainModel(Long userId);

    /**
     * 记录推荐反馈（用于模型优化）
     *
     * @param productId         商品ID
     * @param recommendedStyles 推荐的风格列表
     * @param selectedStyles    用户选择的风格列表
     * @param userId            用户ID
     */
    void recordFeedback(Long productId, List<String> recommendedStyles, List<String> selectedStyles, Long userId);

    /**
     * 获取模型性能指标
     *
     * @param userId 用户ID
     * @return 模型指标
     */
    ModelMetricsVO getModelMetrics(Long userId);

    /**
     * 计算商品特征向量（用于相似度计算）
     *
     * @param productId 商品ID
     * @param userId    用户ID
     * @return 特征向量（JSON字符串）
     */
    String computeProductFeatures(Long productId, Long userId);

    /**
     * 查找相似商品
     *
     * @param productId 商品ID
     * @param userId    用户ID
     * @param topK      返回Top-K个相似商品
     * @return 相似商品列表
     */
    List<StyleRecommendationVO.SimilarProduct> findSimilarProducts(Long productId, Long userId, Integer topK);
}
