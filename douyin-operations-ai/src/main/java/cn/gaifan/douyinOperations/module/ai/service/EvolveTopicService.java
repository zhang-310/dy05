package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;

import java.util.List;
import java.util.Map;

/**
 * 进化主题服务：主题采样、列表、CRUD、去重、分布统计。
 * 从 EvolveEngineService 拆分。
 */
public interface EvolveTopicService {

    AiEvolveTopic saveTopic(AiEvolveTopic topic);

    void deleteTopic(Long id);

    List<AiEvolveTopic> listTopics(Long kbId, Long accountId, boolean scopeGlobal);

    List<Map<String, Object>> getTopicDistribution();

    /** 按归因权重采样主题（供进化任务使用） */
    List<AiEvolveTopic> sampleTopicsWithAttributionWeight(Long kbId, AiKnowledgeBase kb);

    /** 检查新主题是否与已有主题重复 */
    boolean isTopicDuplicate(String newTopic, List<AiEvolveTopic> existing, double threshold);

    /** 解析知识库分类前缀（如 douyin_%, huashu_%） */
    String resolveCategoryPatternForKb(AiKnowledgeBase kb);
}
