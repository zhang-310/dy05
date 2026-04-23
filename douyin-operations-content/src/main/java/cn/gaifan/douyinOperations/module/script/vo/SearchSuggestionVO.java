package cn.gaifan.douyinOperations.module.script.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索建议响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionVO {

    private List<SuggestionItemVO> suggestions;  // 建议词列表
    private List<HotTopicVO> hotTopics;          // 热点话题

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestionItemVO {
        private String text;
        private String type;                     // HISTORY / HOT_TOPIC / RECOMMENDED / SYSTEM
        private Integer popularity;              // 搜索热度
        private Integer resultCount;             // 匹配结果数
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotTopicVO {
        private String topic;
        private Double trendingScore;            // 热度评分 [0-1]
        private Integer relatedScripts;          // 相关脚本数
    }
}
