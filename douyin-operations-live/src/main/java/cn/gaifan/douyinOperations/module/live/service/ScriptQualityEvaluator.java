package cn.gaifan.douyinOperations.module.live.service;

import java.util.Map;

/**
 * 话术质量多维度评估引擎。
 * 7维度10分制：口语化、感染力、引导力、合规性、人设匹配、节奏感、价值密度。
 * 支持 FIRE 法则（现象级IP）和 DEPTH 法则（顶级IP）评估。
 */
public interface ScriptQualityEvaluator {

    /**
     * 多维度评估话术质量
     * @param scriptContent 话术内容
     * @param ipType IP类型（phenomenal/top），为空则通用评估
     * @param userId 用户 ID
     * @return 评估结果 JSON
     */
    Map<String, Object> evaluate(String scriptContent, String ipType, Long userId);
}
