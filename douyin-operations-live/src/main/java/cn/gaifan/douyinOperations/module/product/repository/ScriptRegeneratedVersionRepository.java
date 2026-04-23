package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ScriptRegeneratedVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 重新生成的话术版本数据访问层
 * 继承 JpaRepository 和 JpaSpecificationExecutor，支持动态查询和分页
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Repository
public interface ScriptRegeneratedVersionRepository extends JpaRepository<ScriptRegeneratedVersion, Long>,
        JpaSpecificationExecutor<ScriptRegeneratedVersion> {

    /**
     * 查询特定建议的所有生成版本
     *
     * @param suggestionId 建议 ID
     * @param ownerId 所有者 ID
     * @return 生成版本列表
     */
    List<ScriptRegeneratedVersion> findBySuggestionIdAndOwnerIdOrderByCreatedAtDesc(
            Long suggestionId, Long ownerId);

    /**
     * 查询特定话术版本的生成版本（分页）
     *
     * @param scriptVersionId 话术版本 ID
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 生成版本分页结果
     */
    Page<ScriptRegeneratedVersion> findByScriptVersionIdAndOwnerIdOrderByCreatedAtDesc(
            Long scriptVersionId, Long ownerId, Pageable pageable);

    /**
     * 查询特定建议的所有风格的生成版本
     *
     * @param suggestionId 建议 ID
     * @param ownerId 所有者 ID
     * @return 生成版本列表
     */
    @Query("SELECT r FROM ScriptRegeneratedVersion r WHERE r.suggestionId = :suggestionId " +
            "AND r.ownerId = :ownerId ORDER BY r.aiQualityScore DESC, r.createdAt DESC")
    List<ScriptRegeneratedVersion> findAllStylesByGenerationAndOwnerId(
            @Param("suggestionId") Long suggestionId,
            @Param("ownerId") Long ownerId);

    /**
     * 查询特定话术版本和风格的生成版本
     *
     * @param scriptVersionId 话术版本 ID
     * @param generationStyle 生成风格
     * @param ownerId 所有者 ID
     * @return 生成版本列表
     */
    @Query("SELECT r FROM ScriptRegeneratedVersion r WHERE r.scriptVersionId = :scriptVersionId " +
            "AND r.generationStyle = :generationStyle AND r.ownerId = :ownerId " +
            "ORDER BY r.createdAt DESC")
    List<ScriptRegeneratedVersion> findByScriptVersionIdAndGenerationStyle(
            @Param("scriptVersionId") Long scriptVersionId,
            @Param("generationStyle") String generationStyle,
            @Param("ownerId") Long ownerId);

    /**
     * 查询已审批的生成版本（分页）
     *
     * @param approvalStatus 审批状态
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 生成版本分页结果
     */
    Page<ScriptRegeneratedVersion> findByApprovalStatusAndOwnerIdOrderByApprovedAtDesc(
            String approvalStatus, Long ownerId, Pageable pageable);

    /**
     * 查询已应用的生成版本（分页）
     *
     * @param isApplied 是否已应用
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 生成版本分页结果
     */
    Page<ScriptRegeneratedVersion> findByIsAppliedAndOwnerIdOrderByAppliedAtDesc(
            Boolean isApplied, Long ownerId, Pageable pageable);

    /**
     * 查询质量评分最高的生成版本
     *
     * @param scriptVersionId 话术版本 ID
     * @param ownerId 所有者 ID
     * @return 质量最高的生成版本列表（Top N）
     */
    @Query("SELECT r FROM ScriptRegeneratedVersion r WHERE r.scriptVersionId = :scriptVersionId " +
            "AND r.ownerId = :ownerId ORDER BY r.aiQualityScore DESC LIMIT 5")
    List<ScriptRegeneratedVersion> findTopByScriptVersionIdAndOwnerId(
            @Param("scriptVersionId") Long scriptVersionId,
            @Param("ownerId") Long ownerId);

    /**
     * 查询特定所有者的生成版本（分页）
     *
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 生成版本分页结果
     */
    Page<ScriptRegeneratedVersion> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    /**
     * 查询待审批的生成版本数量
     *
     * @param ownerId 所有者 ID
     * @return 待审批版本数量
     */
    @Query(value = "SELECT COUNT(*) FROM dy_script_regenerated_version " +
            "WHERE approval_status = 'PENDING' AND owner_id = :ownerId AND deleted = 0",
            nativeQuery = true)
    Long countPendingApprovalByOwnerId(@Param("ownerId") Long ownerId);
}
