package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ScriptOptimizationSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 话术优化建议数据访问层
 * 继承 JpaRepository 和 JpaSpecificationExecutor，支持动态查询和分页
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Repository
public interface ScriptOptimizationSuggestionRepository extends JpaRepository<ScriptOptimizationSuggestion, Long>,
        JpaSpecificationExecutor<ScriptOptimizationSuggestion> {

    /**
     * 查询特定分析结果的所有优化建议
     *
     * @param analysisResultId 分析结果 ID
     * @param ownerId 所有者 ID
     * @return 优化建议列表
     */
    List<ScriptOptimizationSuggestion> findByAnalysisResultIdAndOwnerIdOrderByPriorityAscCreatedAtDesc(
            Long analysisResultId, Long ownerId);

    /**
     * 查询特定话术版本的优化建议（分页）
     *
     * @param scriptVersionId 话术版本 ID
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 优化建议分页结果
     */
    Page<ScriptOptimizationSuggestion> findByScriptVersionIdAndOwnerIdOrderByCreatedAtDesc(
            Long scriptVersionId, Long ownerId, Pageable pageable);

    /**
     * 查询特定分析结果的高优先级建议
     *
     * @param analysisResultId 分析结果 ID
     * @param ownerId 所有者 ID
     * @return 高优先级建议列表
     */
    @Query("SELECT s FROM ScriptOptimizationSuggestion s WHERE s.analysisResultId = :analysisResultId " +
            "AND s.ownerId = :ownerId AND (s.priority = 'HIGH' OR s.priority = 'CRITICAL') " +
            "ORDER BY s.priority DESC, s.createdAt DESC")
    List<ScriptOptimizationSuggestion> findHighPrioritySuggestions(
            @Param("analysisResultId") Long analysisResultId,
            @Param("ownerId") Long ownerId);

    /**
     * 查询特定分类的优化建议（分页）
     *
     * @param category 建议分类
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 优化建议分页结果
     */
    Page<ScriptOptimizationSuggestion> findByCategoryAndOwnerIdOrderByCreatedAtDesc(
            String category, Long ownerId, Pageable pageable);

    /**
     * 查询特定采纳状态的建议（分页）
     *
     * @param adoptionStatus 采纳状态
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 优化建议分页结果
     */
    Page<ScriptOptimizationSuggestion> findByAdoptionStatusAndOwnerIdOrderByCreatedAtDesc(
            String adoptionStatus, Long ownerId, Pageable pageable);

    /**
     * 查询待处理的优化建议数量
     *
     * @param ownerId 所有者 ID
     * @return 待处理建议数量
     */
    @Query(value = "SELECT COUNT(*) FROM dy_optimization_suggestion " +
            "WHERE adoption_status = 'PENDING' AND owner_id = :ownerId AND deleted = 0",
            nativeQuery = true)
    Long countPendingByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 查询特定所有者的所有建议（分页）
     *
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 优化建议分页结果
     */
    Page<ScriptOptimizationSuggestion> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    /**
     * 查询特定优先级的建议（分页）
     *
     * @param priority 优先级
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 优化建议分页结果
     */
    Page<ScriptOptimizationSuggestion> findByPriorityAndOwnerIdOrderByCreatedAtDesc(
            String priority, Long ownerId, Pageable pageable);

    /**
     * 查询特定话术版本已采纳的建议
     *
     * @param scriptVersionId 话术版本 ID
     * @param ownerId 所有者 ID
     * @return 已采纳建议列表
     */
    @Query("SELECT s FROM ScriptOptimizationSuggestion s WHERE s.scriptVersionId = :scriptVersionId " +
            "AND s.ownerId = :ownerId AND s.adoptionStatus != 'PENDING' ORDER BY s.adoptedAt DESC")
    List<ScriptOptimizationSuggestion> findAdoptedSuggestions(
            @Param("scriptVersionId") Long scriptVersionId,
            @Param("ownerId") Long ownerId);
}
