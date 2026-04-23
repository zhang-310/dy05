package cn.gaifan.douyinOperations.module.product.service;

import java.util.List;

/**
 * 产品话术 AI 生成服务接口。
 * 解耦 Product 模块对 Live 模块的直接依赖，由 Live 模块提供实现。
 */
public interface ProductAiService {

    /**
     * 生成产品话术（按人设+风格+时长+场景）
     *
     * @param productId    产品 ID
     * @param scriptType   话术类型：seed/promotion/formal
     * @param style        风格：professional/friendly/passionate 等
     * @param personaId    人设 ID（可选）
     * @param duration     时长（秒）
     * @param userId       当前用户 ID
     * @param useKbRef     是否使用话术知识库参考（RAG），null 或 true 时使用
     * @param scene        应用场景：short_video/guopin/cangbo/danpin/yubo（可选）
     * @param kbCategories 话术知识库参考分类（可选，多选）
     * @return 生成结果
     */
    ProductScriptAiResult generateScript(Long productId, String scriptType, String style,
            Long personaId, int duration, Long userId, Boolean useKbRef, String scene, List<String> kbCategories);

    /**
     * 产品话术 AI 生成结果
     */
    record ProductScriptAiResult(String scriptContent, Integer tokenUsage) {}
}
