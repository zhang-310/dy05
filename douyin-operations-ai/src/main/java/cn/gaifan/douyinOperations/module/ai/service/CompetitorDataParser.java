package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 竞品数据解析器：原始文本 → 清洗 → 结构化 → 质量评分
 */
public interface CompetitorDataParser {

    /**
     * 解析原始文本为结构化竞品洞察
     *
     * @param rawText    原始文本
     * @param dataSource 数据源标识
     * @param category   商品品类
     * @return 结构化洞察列表 [{insightType, content, qualityScore, dimension}]
     */
    List<Map<String, Object>> parseToInsights(String rawText, String dataSource, String category);

    /**
     * 评估洞察质量评分
     *
     * @param content 洞察内容
     * @return 0.0 ~ 1.0
     */
    double scoreQuality(String content);
}
