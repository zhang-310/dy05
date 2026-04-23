package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 知识内容效果统计服务（阶段一）
 * 用于 viral/live_review 迭代闭环的数据打点
 */
public interface ContentEffectivenessService {

    /**
     * 记录检索命中：hybridSearch 返回结果时调用
     * @param docIds 命中的文档 ID（去重后的集合）
     */
    void recordRetrieval(Collection<Long> docIds);

    /**
     * 记录进化引用：gatherContext 用作进化上下文时调用
     * @param docIds 被引用的文档 ID（去重后的集合）
     */
    void recordCitation(Collection<Long> docIds);

    /**
     * 按来源类型统计效果（用于 Prompt 迭代、仪表盘）
     * @param sourceType manual/evolved/viral/live_review
     * @return 统计结果：总文档数、总检索次数、总引用次数、Top 引用 doc 列表等
     */
    Map<String, Object> getEffectivenessBySourceType(String sourceType);

    /**
     * 获取高引用文档摘要（用于 Prompt 注入，阶段二）
     * @param sourceType viral_analysis 或 live_review
     * @param topN 取前 N 条
     * @return 文档 ID、标题、引用次数、内容摘要
     */
    List<Map<String, Object>> getTopCitedDocs(String sourceType, int topN);
}
