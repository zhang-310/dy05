package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkScriptSimilarityVO;

import java.util.List;
import java.util.Map;

/**
 * 质量脚本推荐引擎服务
 */
public interface BenchmarkScriptRecommendationService {

    /**
     * 根据用户需求推荐脚本
     *
     * @param requirement 用户需求描述
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个推荐
     * @return 推荐脚本列表
     */
    List<BenchmarkScriptSimilarityVO> recommendByRequirement(String requirement, Long ownerId, Integer topK);

    /**
     * 根据行业和场景推荐脚本
     *
     * @param industry 行业
     * @param sceneType 场景类型
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个推荐
     * @return 推荐脚本列表
     */
    List<BenchmarkScriptSimilarityVO> recommendByIndustryAndScene(String industry, String sceneType, Long ownerId, Integer topK);

    /**
     * 根据脚本类型推荐相似脚本
     *
     * @param scriptType 脚本类型
     * @param referenceScriptId 参考脚本 ID（可选）
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个推荐
     * @return 推荐脚本列表
     */
    List<BenchmarkScriptSimilarityVO> recommendByScriptType(String scriptType, Long referenceScriptId, Long ownerId, Integer topK);

    /**
     * 智能推荐：综合多维度推荐
     *
     * @param filters 过滤条件（industry, sceneType, scriptType, minQualityScore 等）
     * @param referenceText 参考文本（可选）
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个推荐
     * @return 推荐脚本列表
     */
    List<BenchmarkScriptSimilarityVO> smartRecommend(Map<String, Object> filters, String referenceText, Long ownerId, Integer topK);

    /**
     * 获取热门脚本推荐（基于引用次数和质量评分）
     *
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个推荐
     * @return 推荐脚本列表
     */
    List<BenchmarkScriptSimilarityVO> getPopularScripts(Long ownerId, Integer topK);

    /**
     * 获取最新高质量脚本推荐
     *
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个推荐
     * @param minQualityScore 最低质量评分
     * @return 推荐脚本列表
     */
    List<BenchmarkScriptSimilarityVO> getLatestQualityScripts(Long ownerId, Integer topK, Double minQualityScore);

    /**
     * 根据视频分析结果推荐改进脚本
     *
     * @param analysisId 分析 ID
     * @param ownerId 用户 ID
     * @param topK 返回前 K 个推荐
     * @return 推荐脚本列表
     */
    List<BenchmarkScriptSimilarityVO> recommendImprovementScripts(Long analysisId, Long ownerId, Integer topK);
}
