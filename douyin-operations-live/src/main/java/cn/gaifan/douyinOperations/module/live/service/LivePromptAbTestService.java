package cn.gaifan.douyinOperations.module.live.service;

import java.util.Map;

/**
 * Prompt A/B 测试服务
 * <p>
 * 复用 ab_experiment 框架，新增 prompt_variant 维度：
 * - 同一槽位生成 2 个版本话术（不同 prompt 策略）
 * - 由主播选用后自动记录效果
 * - 闭环优化 prompt 参数
 * <p>
 * 实验类型：experiment_type = 'live_prompt'
 * 变体维度：variant_key = liveFormat | scriptType | style 组合
 * 效果指标：effectiveness_score / gmv_delta / interaction_delta
 */
public interface LivePromptAbTestService {

    /**
     * Prompt 变体定义
     */
    record PromptVariant(
            /** 变体 ID（来自 ab_experiment_variant.id） */
            Long variantId,
            /** 变体名称（如 "v1_标准" / "v2_强情绪" ） */
            String variantName,
            /** 系统提示差异点（覆盖 liveFormat 的默认 system prompt 局部修改） */
            String systemPromptOverride,
            /** 用户提示附加指令（追加到 buildPrompt 输出后） */
            String userPromptAppend,
            /** 流量权重（0.0-1.0，多个变体之和=1.0） */
            double trafficWeight
    ) {}

    /**
     * 为指定生成场景查找当前活跃的 Prompt A/B 实验变体
     * <p>
     * 如果没有活跃实验，返回空 Map（走默认 prompt 逻辑）
     *
     * @param userId     用户 ID
     * @param sessionId  场次 ID
     * @param scriptType 话术类型
     * @param liveFormat 直播形式
     * @return key=variantKey, value=PromptVariant；空表示无活跃实验
     */
    Map<String, PromptVariant> getActiveVariants(Long userId, Long sessionId, String scriptType, String liveFormat);

    /**
     * 按流量权重随机选择一个变体
     *
     * @param variants 候选变体（来自 getActiveVariants）
     * @return 选中的变体，若无候选则返回 null
     */
    PromptVariant pickVariant(Map<String, PromptVariant> variants);

    /**
     * 记录变体的话术效果（由归因服务在话术结束后调用）
     *
     * @param variantId          变体 ID
     * @param scriptId           话术 ID
     * @param effectivenessScore 效果分（0-100）
     * @param gmvDelta           GMV 增量
     */
    void recordVariantResult(Long variantId, Long scriptId, double effectivenessScore, double gmvDelta);

    /**
     * 创建一个 Prompt A/B 实验（供管理后台调用）
     *
     * @param ownerId        创建者 ID
     * @param name           实验名称
     * @param scriptType     测试的话术类型
     * @param liveFormat     测试的直播形式
     * @param variantConfigs 变体配置列表（Key=variantName, Value=systemPromptOverride）
     * @return 实验 ID
     */
    Long createPromptExperiment(Long ownerId, String name, String scriptType, String liveFormat,
                                 Map<String, String> variantConfigs);
}
