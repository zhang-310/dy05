package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

public interface CompetitorInsightService {
    void collectInsights();
    void ingestHighQualityToKb();

    /**
     * 获取同品类差异化建议
     *
     * @param category 品类名称
     * @return 差异化建议文本
     */
    String getDifferentiationAdvice(String category);

    /**
     * 获取同品类最近 N 天的竞品洞察摘要
     *
     * @param category 品类名称
     * @param days     天数
     * @return 洞察摘要列表
     */
    List<Map<String, Object>> getRecentInsights(String category, int days);
}
