package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkQualityScriptVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 质量脚本知识库服务
 */
public interface BenchmarkQualityScriptService {

    /**
     * 分页查询质量脚本列表
     */
    PageResultVO<BenchmarkQualityScriptVO> search(BenchmarkQualityScriptSearchVO searchVO, Long ownerId);

    /**
     * 根据 ID 查询质量脚本详情
     */
    BenchmarkQualityScriptVO getById(Long id, Long ownerId);

    /**
     * 保存质量脚本（新增或更新）
     */
    BenchmarkQualityScriptVO save(BenchmarkQualityScriptSaveVO saveVO, Long ownerId);

    /**
     * 删除质量脚本
     */
    void delete(Long id, Long ownerId);

    /**
     * 根据视频 ID 查询质量脚本
     */
    BenchmarkQualityScriptVO getByVideoId(Long videoId, Long ownerId);

    /**
     * 根据分析 ID 查询质量脚本
     */
    BenchmarkQualityScriptVO getByAnalysisId(Long analysisId, Long ownerId);

    /**
     * 获取指定行业的高质量脚本（质量分 >= 阈值）
     */
    List<BenchmarkQualityScriptVO> getHighQualityScriptsByIndustry(String industry, BigDecimal minScore, Long ownerId);

    /**
     * 获取指定场景类型的高质量脚本
     */
    List<BenchmarkQualityScriptVO> getHighQualityScriptsByScene(String sceneType, BigDecimal minScore, Long ownerId);

    /**
     * 获取互动率最高的脚本（Top N）
     */
    List<BenchmarkQualityScriptVO> getTopEngagementScripts(Integer limit, Long ownerId);

    /**
     * 获取传播力最高的脚本（Top N）
     */
    List<BenchmarkQualityScriptVO> getTopViralScripts(Integer limit, Long ownerId);

    /**
     * 更新脚本引用统计
     */
    void updateReferenceStats(Long scriptId);

    /**
     * 计算质量评分（综合算法）
     */
    BigDecimal calculateQualityScore(BenchmarkQualityScriptSaveVO saveVO);
}
