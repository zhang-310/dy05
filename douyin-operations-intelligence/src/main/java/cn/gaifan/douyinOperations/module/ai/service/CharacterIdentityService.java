package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 角色身份管理服务 (Phase 8)
 * LoRA 训练 + 加权 Prompt + 多参考图，90%+ 一致性
 */
public interface CharacterIdentityService {

    /**
     * 根据角色参考图生成加权 Prompt
     *
     * @param characterId 角色 ID
     * @param basePrompt  基础 prompt
     * @return 加权后的 prompt
     */
    String buildWeightedPrompt(Long characterId, String basePrompt);

    /**
     * 获取角色多参考图 URL 列表
     */
    List<String> getReferenceImageUrls(Long characterId);

    /**
     * 检查 LoRA 训练状态
     *
     * @param characterId 角色 ID
     * @return 状态: pending/training/ready/failed
     */
    String getLoraStatus(Long characterId);

    /**
     * 提交 LoRA 训练任务（占位，需接入训练服务）
     */
    String submitLoraTraining(Long characterId, List<String> referenceImageUrls);
}
