package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptVO;

/**
 * 质量脚本自动入库服务
 */
public interface BenchmarkScriptAutoIngestService {

    /**
     * 分析完成后自动入库
     * 根据质量评分阈值判断是否入库
     *
     * @param analysis 分析结果
     * @param ownerId 用户 ID
     * @return 入库的质量脚本（如果符合条件），否则返回 null
     */
    BenchmarkQualityScriptVO autoIngestAfterAnalysis(BenchmarkAnalysis analysis, Long ownerId);

    /**
     * 批量自动入库
     *
     * @param analysisIds 分析 ID 列表
     * @param ownerId 用户 ID
     * @return 成功入库的数量
     */
    Integer batchAutoIngest(java.util.List<Long> analysisIds, Long ownerId);

    /**
     * 检查是否符合入库条件
     *
     * @param analysis 分析结果
     * @return true 符合条件，false 不符合
     */
    boolean isQualifiedForIngest(BenchmarkAnalysis analysis);

    /**
     * 从分析结果提取质量指标
     *
     * @param analysis 分析结果
     * @return 质量评分（0-100）
     */
    java.math.BigDecimal extractQualityScore(BenchmarkAnalysis analysis);

    /**
     * 获取入库阈值配置
     *
     * @return 质量评分阈值（默认 70.0）
     */
    java.math.BigDecimal getIngestThreshold();

    /**
     * 设置入库阈值配置
     *
     * @param threshold 质量评分阈值
     */
    void setIngestThreshold(java.math.BigDecimal threshold);
}
