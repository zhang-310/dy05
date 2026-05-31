package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.List;
import java.util.Map;

/**
 * 行业知识图谱服务（Phase1）
 * 实体：主播/产品/话题/玩法/平台规则
 * 关系：擅长/使用/关联/竞争/衍生
 * 属性：热度/时效/难度/成功率
 */
public interface IndustryKnowledgeGraphService {

    /**
     * 查询实体及其关系
     *
     * @param entityType 实体类型：anchor/product/topic/play_style/platform_rule
     * @param keyword    关键词
     * @param limit      返回数量
     * @return 实体列表及关系
     */
    List<Map<String, Object>> queryEntities(String entityType, String keyword, int limit);

    /**
     * 查询实体及其关系（带 owner 作用域）
     */
    default List<Map<String, Object>> queryEntities(String entityType, String keyword, int limit, Long ownerId) {
        return queryEntities(entityType, keyword, limit);
    }

    /**
     * 获取实体详情及关联
     */
    Map<String, Object> getEntityDetail(String entityId, String entityType);

    /**
     * 是否可用（Neo4j/ES 等后端是否就绪）
     */
    boolean isAvailable();

    /**
     * GraphRAG 融合：从 query 提取实体，按配置跳数遍历出边（默认 2 跳，{@code app.ai.brain.knowledge-graph.max-hops}），返回可拼接到 query 的上下文字符串。
     *
     * @param query   用户查询
     * @param ownerId 租户 ID（0=全局）
     * @param limit   最多返回的路径条数（每跳一段 "实体-关系-实体"）
     * @return 如 "[相关知识：玻尿酸-treats-干燥, 玻尿酸-treats-干燥-suits-干性肌肤]" 或空字符串
     */
    String getGraphContextForQuery(String query, Long ownerId, int limit);

    /**
     * G-5/G-6/G-2：返回与查询相关的子图 JSON（nodes / edges / contradictions），无数据时 nodes、edges 为空列表。
     */
    Map<String, Object> getGraphJsonForQuery(String query, Long ownerId, int limit);

    /**
     * G-2：关系建议 / 人工校验队列（弱监督、规则产出写入 {@code ai_graph_relation_suggestion}）。
     */
    List<Map<String, Object>> listRelationSuggestions(Long ownerId, String status, int limit);

    /** G-2：批量落库 pending（去重 source+target+type）；返回新建条数 */
    int materializeRelationSuggestions(Long ownerId, List<Map<String, Object>> pairs);

    /** G-2：审核更新 {@code status}（如 approved / rejected） */
    void updateRelationSuggestionStatus(Long ownerId, Long id, String newStatus);
}
