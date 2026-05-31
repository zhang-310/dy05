package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.vo.*;

/**
 * 商品话术版本业务逻辑接口
 *
 * @author Claude Code
 * @since 2026-03-06
 */
public interface ProductScriptVersionService {

    /**
     * 保存或更新话术版本
     *
     * @param vo 保存参数
     * @param userId 当前用户 ID（作为 owner_id）
     * @return 话术版本返回值
     */
    ProductScriptVersionVO save(ProductScriptVersionSaveVO vo, Long userId);

    /**
     * 分页查询话术版本
     *
     * @param searchVO 查询参数
     * @param userId 当前用户 ID（数据隔离）
     * @return 分页结果
     */
    PageResultVO<ProductScriptVersionVO> list(ProductScriptVersionSearchVO searchVO, Long userId);

    /**
     * 搜索话术版本
     *
     * @param keyword 搜索关键词
     * @param style 话术风格（可选）
     * @param minScore 最小效果评分（可选）
     * @param page 页码（0-indexed）
     * @param rows 每页行数
     * @param userId 当前用户 ID（数据隔离）
     * @return 分页结果
     */
    PageResultVO<ProductScriptVersionVO> search(String keyword, String style,
                                                Double minScore, Integer page, Integer rows, Long userId);

    /**
     * 获取话术版本详情
     *
     * @param id 话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 话术版本详情
     */
    ProductScriptVersionVO getDetail(Long id, Long userId);

    /**
     * 获取产品的推荐话术版本
     *
     * @param productId 产品 ID
     * @param topN 推荐数量
     * @param userId 当前用户 ID（数据隔离）
     * @return 推荐结果
     */
    ProductScriptRecommendVO recommend(Long productId, Integer topN, Long userId);

    /**
     * 更新话术效果评分
     *
     * @param scriptVersionId 话术版本 ID
     * @param effectivenessScore 效果评分
     * @param conversionRate 转化率
     * @param userId 当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    ProductScriptVersionVO updateEffectiveness(Long scriptVersionId, Double effectivenessScore,
                                               Double conversionRate, Long userId);

    /**
     * 从话术库引用到直播场次
     *
     * @param liveSessionId 直播场次 ID
     * @param productScriptVersionId 话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 快照结果
     */
    ProductScriptSnapshotVO applyFromLibrary(Long liveSessionId, Long productScriptVersionId, Long userId);

    /**
     * 删除话术版本
     *
     * @param id 话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 是否删除成功
     */
    boolean delete(Long id, Long userId);

    /**
     * 更新话术版本状态
     *
     * @param id 话术版本 ID
     * @param isActive 是否启用
     * @param userId 当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    ProductScriptVersionVO updateStatus(Long id, Boolean isActive, Long userId);

    /**
     * 获取特定产品的所有话术版本
     *
     * @param productId 产品 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 话术版本列表
     */
    java.util.List<ProductScriptVersionVO> listByProductId(Long productId, Long userId);

    /**
     * 获取产品的最优版本
     *
     * @param productId 产品 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 最优话术版本
     */
    ProductScriptVersionVO findBestVersion(Long productId, Long userId);

    /**
     * 增加话术使用次数
     *
     * @param id 话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    ProductScriptVersionVO increaseUsageCount(Long id, Long userId);

    /**
     * 更新推荐标记
     *
     * @param id 话术版本 ID
     * @param isRecommended 是否推荐
     * @param userId 当前用户 ID（数据隔离）
     * @return 更新后的话术版本
     */
    ProductScriptVersionVO updateRecommendFlag(Long id, Boolean isRecommended, Long userId);

    /**
     * 计算推荐分数
     *
     * @param effectivenessScore 效果评分
     * @param usageCount 使用次数
     * @param conversionRate 转化率
     * @return 推荐分数
     */
    double calculateRecommendScore(double effectivenessScore, int usageCount, double conversionRate);

    /** 对比两个话术版本的差异（行级 added/removed/changed） */
    ProductVersionDiffVO diffVersions(Long productId, Integer versionA, Integer versionB);

    /**
     * 为商品主话术确保存在一条可进入优化链路的版本镜像。
     */
    EnsureOptimizationVersionResultVO ensureOptimizationVersion(
            EnsureOptimizationVersionVO vo, Long userId);
}
