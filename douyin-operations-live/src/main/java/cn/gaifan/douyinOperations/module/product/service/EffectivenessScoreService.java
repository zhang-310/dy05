package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.vo.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品话术效果评分业务逻辑接口
 *
 * @author Claude Code
 * @since 2026-03-06
 */
public interface EffectivenessScoreService {

    /**
     * 计算指定话术版本的效果评分
     * 评分算法：base_score(50) + use_score(max 25) + conversion_score(max 15) + interaction_score(max 10)
     *
     * @param versionId 话术版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 评分值（0-100）
     */
    Double calculateScore(Long versionId, Long userId);

    /**
     * 重新计算产品的所有版本的效果评分
     *
     * @param productId 产品 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 更新的版本数量
     */
    Integer recalculateAllScores(Long productId, Long userId);

    /**
     * 获取产品的话术版本排行榜
     *
     * @param productId 产品 ID
     * @param topN 获取前 N 名（0 表示所有）
     * @param sortBy 排序字段（score/usage/conversion/interaction）
     * @param page 页码（0-indexed）
     * @param rows 每页行数
     * @param userId 当前用户 ID（数据隔离）
     * @return 排行榜结果
     */
    PageResultVO<ScriptRankingVO> getRanking(Long productId, Integer topN, String sortBy,
                                              Integer page, Integer rows, Long userId);

    /**
     * 对比指定的多个话术版本
     *
     * @param versionIds 版本 ID 列表（最多 5 个）
     * @param userId 当前用户 ID（数据隔离）
     * @return 对比结果
     */
    ScriptComparisonVO compareVersions(List<Long> versionIds, Long userId);

    /**
     * 获取指定话术版本的历史趋势
     *
     * @param versionId 版本 ID
     * @param days 查询天数
     * @param userId 当前用户 ID（数据隔离）
     * @return 趋势数据
     */
    ScriptTrendVO getTrend(Long versionId, Integer days, Long userId);

    /**
     * 获取产品不同风格的对比分析
     *
     * @param productId 产品 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 风格对比结果
     */
    ScriptComparisonVO getStyleComparison(Long productId, Long userId);

    /**
     * 记录快照到评分历史表
     *
     * @param versionId 版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 是否记录成功
     */
    boolean recordSnapshot(Long versionId, Long userId);

    /**
     * 清除指定产品的对比缓存
     *
     * @param productId 产品 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 清除的缓存条数
     */
    Integer clearComparisonCache(Long productId, Long userId);

    /**
     * 获取效果分析汇总
     *
     * @param versionId 版本 ID
     * @param userId 当前用户 ID（数据隔离）
     * @return 分析结果
     */
    ScriptEffectivenessAnalysisVO getAnalysis(Long versionId, Long userId);

    /**
     * 判定评分等级
     *
     * @param score 评分值（0-100）
     * @return 等级（A/B/C/D/F）
     */
    String determineScoreLevel(Double score);

    /**
     * 获取推荐语句
     *
     * @param scoreLevel 评分等级
     * @return 推荐语句
     */
    String getRecommendation(String scoreLevel);
}
