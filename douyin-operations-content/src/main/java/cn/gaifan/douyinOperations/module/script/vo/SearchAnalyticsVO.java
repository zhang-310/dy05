package cn.gaifan.douyinOperations.module.script.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索分析结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchAnalyticsVO {

    private String analyticsDate;              // 分析日期
    private String searchQuery;                // 搜索查询
    private String searchType;                 // 搜索类型: HYBRID / SEMANTIC / LEXICAL
    private Integer searchCount;               // 搜索次数
    private Double avgExecutionTimeMs;         // 平均执行耗时
    private Double clickThroughRate;           // 点击率 [0-100]
    private Double satisfactionScore;          // 满意度评分 [0-1]
    private Long topResultId;                  // 排名第一的脚本 ID
    private Integer topResultClickCount;       // 第一名点击次数
}
