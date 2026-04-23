package cn.gaifan.douyinOperations.module.script.service;

import cn.gaifan.douyinOperations.module.script.vo.SearchSuggestionVO;

import java.util.List;

/**
 * 搜索建议服务接口
 */
public interface SearchSuggestionService {

    /**
     * 获取搜索建议（自动补全）
     */
    SearchSuggestionVO getSuggestions(String prefix, Long userId, Integer limit);

    /**
     * 获取热点话题
     */
    List<SearchSuggestionVO.HotTopicVO> getHotTopics(Long userId, Integer limit);

    /**
     * 更新搜索建议词库
     */
    void updateSuggestions();

    /**
     * 更新热度评分
     */
    void updateTrendingScores();

    /**
     * 记录搜索查询（用于统计热点词）
     */
    void recordSearchQuery(String query, Long userId);

    /**
     * 添加自定义建议词
     */
    void addCustomSuggestion(String text, Long userId);

    /**
     * 删除建议词
     */
    void removeSuggestion(Long suggestionId);
}
