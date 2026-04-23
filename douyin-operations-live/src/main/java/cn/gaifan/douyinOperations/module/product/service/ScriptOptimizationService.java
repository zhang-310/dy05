package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.vo.OptimizationSuggestionVO;
import cn.gaifan.douyinOperations.module.product.vo.RegeneratedScriptVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptAnalysisResultVO;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import java.util.List;

/**
 * 话术优化建议服务接口
 *
 * @author Claude Code
 * @since 2026-03-06
 */
public interface ScriptOptimizationService {

    /**
     * 分析话术效果
     *
     * @param scriptVersionId 话术版本 ID
     * @param dataSource 数据源（LIVE_MONITOR/HISTORICAL）
     * @param analysisType 分析类型（COMPREHENSIVE/INTERACTION/CONVERSION/COMMENT）
     * @param userId 用户 ID（作为所有者）
     * @return 分析结果 VO
     */
    ScriptAnalysisResultVO analyzeScript(Long scriptVersionId, String dataSource, String analysisType, Long userId);

    /**
     * 获取优化建议
     *
     * @param scriptVersionId 话术版本 ID
     * @param analysisResultId 分析结果 ID
     * @param topN 返回建议数量（降序按优先级）
     * @param userId 用户 ID（作为所有者）
     * @return 优化建议列表
     */
    List<OptimizationSuggestionVO> getOptimizationSuggestions(Long scriptVersionId, Long analysisResultId,
                                                              Integer topN, Long userId);

    /**
     * 重新生成话术
     *
     * @param scriptVersionId 话术版本 ID
     * @param suggestionId 建议 ID
     * @param generationStyles 生成的话术风格列表
     * @param userId 用户 ID（作为所有者）
     * @return 生成的话术版本列表
     */
    List<RegeneratedScriptVO> regenerateScript(Long scriptVersionId, Long suggestionId,
                                                List<String> generationStyles, Long userId);

    /**
     * 查看优化历史
     *
     * @param scriptVersionId 话术版本 ID
     * @param page 页码（0-indexed）
     * @param rows 每页行数
     * @param userId 用户 ID（作为所有者）
     * @return 优化历史分页结果
     */
    PageResultVO<ScriptAnalysisResultVO> getOptimizationHistory(Long scriptVersionId, Integer page,
                                                                 Integer rows, Long userId);

    /**
     * 采纳优化建议
     *
     * @param suggestionId 建议 ID
     * @param userId 用户 ID（作为所有者）
     * @return 是否采纳成功
     */
    Boolean acceptSuggestion(Long suggestionId, Long userId);

    /**
     * 拒绝优化建议
     *
     * @param suggestionId 建议 ID
     * @param notes 拒绝原因
     * @param userId 用户 ID（作为所有者）
     * @return 是否拒绝成功
     */
    Boolean rejectSuggestion(Long suggestionId, String notes, Long userId);

    /**
     * 应用重新生成的话术版本
     *
     * @param regeneratedVersionId 生成版本 ID
     * @param userId 用户 ID（作为所有者）
     * @return 是否应用成功
     */
    Boolean applyRegeneratedVersion(Long regeneratedVersionId, Long userId);

    /**
     * 批准重新生成的话术版本
     *
     * @param regeneratedVersionId 生成版本 ID
     * @param approverUserId 审批人 ID
     * @param notes 审批备注
     * @return 是否审批成功
     */
    Boolean approveRegeneratedVersion(Long regeneratedVersionId, Long approverUserId, String notes);

    /**
     * 拒绝重新生成的话术版本
     *
     * @param regeneratedVersionId 生成版本 ID
     * @param approverUserId 审批人 ID
     * @param notes 拒绝原因
     * @return 是否拒绝成功
     */
    Boolean rejectRegeneratedVersion(Long regeneratedVersionId, Long approverUserId, String notes);
}
