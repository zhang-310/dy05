package cn.gaifan.douyinOperations.module.script.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 混合搜索结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridSearchResultVO {

    private Long total;                        // 总结果数
    private Integer pageNum;                   // 当前页
    private Integer pageSize;                  // 每页大小
    private List<SearchItemVO> list;           // 搜索结果列表
    private Long searchTime;                   // 搜索耗时 (ms)
    private String executedAt;                 // 执行时间

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchItemVO {
        private Long scriptId;
        private String title;
        private String content;
        private String category;
        private String style;
        private String author;
        private BigDecimal vectorScore;        // 向量相似度 [0-1]
        private BigDecimal lexicalScore;       // BM25 分数 [0-1]
        private BigDecimal hybridScore;        // RRF 融合分数
        private BigDecimal effectivenessScore; // 话术效果评分
        private Long usageCount;               // 使用次数
        private Long favoriteCount;            // 收藏数
        private String createdAt;              // 创建时间
    }
}
