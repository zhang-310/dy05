package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;

import java.util.List;
import java.util.Map;

/**
 * 直播话术 Prompt 构建服务：RAG 上下文、知识库引用、检索查询构建。
 * 从 LiveScriptGenerationServiceImpl 拆分。
 */
public interface LiveScriptPromptService {

    /**
     * RAG 上下文构建结果
     */
    record RagContextResult(String xml, List<KnowledgeBaseService.SearchResult> refs) {}

    /**
     * 批量 RAG 上下文构建结果：按 scriptType 分组缓存，全场生成时调用一次
     */
    record BatchRagContextResult(Map<String, RagContextResult> byScriptType) {
        /** 根据 scriptType 获取对应 RAG 上下文，未命中时返回 null */
        public RagContextResult get(String scriptType) {
            if (byScriptType == null || scriptType == null) return null;
            RagContextResult exact = byScriptType.get(scriptType);
            if (exact != null) return exact;
            // 降级：用 "product" 类型的 RAG 结果作为其它商品类型的 fallback
            if (scriptType.startsWith("product") || "closing_deal".equals(scriptType)
                    || "pain_point".equals(scriptType) || "testimony".equals(scriptType)
                    || "deep_sell".equals(scriptType)) {
                return byScriptType.get("product");
            }
            return null;
        }
    }

    /**
     * 构建 RAG 上下文（话术知识库检索，注入参考案例）
     *
     * @param userId      用户 ID
     * @param product     产品（product 话术时非空）
     * @param scriptType  话术类型
     * @param style       风格
     * @param promptLength 已有 prompt 长度（用于预算）
     * @param requirement 需求描述
     * @return RAG 上下文，无匹配时返回 null
     */
    RagContextResult buildRagContext(Long userId, DyProduct product, String scriptType,
                                      String style, int promptLength, String requirement);

    /**
     * 构建 RAG 上下文，并按用户选择的素材类型（如顺口溜/金句/段子）额外检索 TianAPI 素材。
     */
    default RagContextResult buildRagContext(Long userId, DyProduct product, String scriptType,
                                             String style, int promptLength, String requirement,
                                             String materialType) {
        return buildRagContext(userId, product, scriptType, style, promptLength, requirement);
    }

    /**
     * 批量构建 RAG 上下文（全场生成优化）
     * <p>
     * 将多个槽位所需的 RAG 查询合并为一次知识库检索，再按 scriptType 分发结果，
     * 避免全场生成时 N 次独立查询（O(N) 问题）。
     *
     * @param userId          用户 ID
     * @param style           统一风格
     * @param scriptTypes     需要检索的话术类型集合（去重）
     * @param productForRag   代表性商品（用于商品话术 RAG 查询，可为 null）
     * @return 批量结果，key=scriptType，value=对应 RAG 上下文
     */
    BatchRagContextResult buildRagContextBatch(Long userId, String style,
                                                List<String> scriptTypes, DyProduct productForRag);
}
