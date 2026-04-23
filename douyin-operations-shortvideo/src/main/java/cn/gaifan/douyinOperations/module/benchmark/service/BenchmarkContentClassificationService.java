package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.vo.ContentClassificationVO;

/**
 * 内容分类验证服务
 *
 * 功能：
 * 1. AI自动分类验证
 * 2. 关键词匹配度检测
 * 3. 分类纠错建议
 */
public interface BenchmarkContentClassificationService {

    /**
     * 验证内容与目标分类的相关度
     *
     * @param scriptContent 脚本内容
     * @param targetIndustry 目标行业（如：护肤、彩妆、人生感悟）
     * @param targetSceneType 目标场景（如：直播、短视频）
     * @param ownerId 用户ID
     * @return 分类验证结果（包含相关度分数、建议分类、是否需要人工审核）
     */
    ContentClassificationVO validateClassification(
            String scriptContent,
            String targetIndustry,
            String targetSceneType,
            Long ownerId
    );

    /**
     * AI自动分类
     *
     * @param scriptContent 脚本内容
     * @param ownerId 用户ID
     * @return 分类结果（行业、场景、脚本类型）
     */
    ContentClassificationVO autoClassify(String scriptContent, Long ownerId);

    /**
     * 批量验证分类
     *
     * @param scriptIds 脚本ID列表
     * @param ownerId 用户ID
     * @return 验证结果列表
     */
    java.util.List<ContentClassificationVO> batchValidate(
            java.util.List<Long> scriptIds,
            Long ownerId
    );

    /**
     * 获取行业分类字典
     *
     * @return 行业分类列表（护肤、彩妆、人生感悟、搞笑段子等）
     */
    java.util.List<String> getIndustryCategories();

    /**
     * 获取场景类型字典
     *
     * @return 场景类型列表（直播、短视频、图文等）
     */
    java.util.List<String> getSceneTypes();

    /**
     * 获取脚本类型字典
     *
     * @return 脚本类型列表（产品介绍、情感共鸣、知识科普等）
     */
    java.util.List<String> getScriptTypes();

    /**
     * 标记为需要人工审核
     *
     * @param scriptId 脚本ID
     * @param reason 审核原因
     * @param ownerId 用户ID
     */
    void markForManualReview(Long scriptId, String reason, Long ownerId);

    /**
     * 人工确认分类
     *
     * @param scriptId 脚本ID
     * @param confirmedIndustry 确认的行业
     * @param confirmedSceneType 确认的场景
     * @param confirmedScriptType 确认的脚本类型
     * @param ownerId 用户ID
     */
    void confirmClassification(
            Long scriptId,
            String confirmedIndustry,
            String confirmedSceneType,
            String confirmedScriptType,
            Long ownerId
    );
}
