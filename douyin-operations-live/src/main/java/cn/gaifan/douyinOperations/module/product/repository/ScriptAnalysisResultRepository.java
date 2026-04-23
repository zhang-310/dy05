package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ScriptAnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 话术分析结果数据访问层
 * 继承 JpaRepository 和 JpaSpecificationExecutor，支持动态查询和分页
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Repository
public interface ScriptAnalysisResultRepository extends JpaRepository<ScriptAnalysisResult, Long>,
        JpaSpecificationExecutor<ScriptAnalysisResult> {

    /**
     * 查询特定话术版本的所有分析结果
     *
     * @param scriptVersionId 话术版本 ID
     * @param ownerId 所有者 ID
     * @return 分析结果列表
     */
    List<ScriptAnalysisResult> findByScriptVersionIdAndOwnerId(Long scriptVersionId, Long ownerId);

    /**
     * 查询特定话术版本的分析结果（分页）
     *
     * @param scriptVersionId 话术版本 ID
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 分析结果分页结果
     */
    Page<ScriptAnalysisResult> findByScriptVersionIdAndOwnerIdOrderByCreatedAtDesc(
            Long scriptVersionId, Long ownerId, Pageable pageable);

    /**
     * 查询特定话术版本的最新分析结果
     *
     * @param scriptVersionId 话术版本 ID
     * @param ownerId 所有者 ID
     * @return 最新分析结果，Optional 包装
     */
    @Query("SELECT a FROM ScriptAnalysisResult a WHERE a.scriptVersionId = :scriptVersionId " +
            "AND a.ownerId = :ownerId ORDER BY a.createdAt DESC LIMIT 1")
    Optional<ScriptAnalysisResult> findLatestByScriptVersionIdAndOwnerId(
            @Param("scriptVersionId") Long scriptVersionId,
            @Param("ownerId") Long ownerId);

    /**
     * 查询特定分析类型的分析结果（分页）
     *
     * @param analysisType 分析类型
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 分析结果分页结果
     */
    Page<ScriptAnalysisResult> findByAnalysisTypeAndOwnerIdOrderByCreatedAtDesc(
            String analysisType, Long ownerId, Pageable pageable);

    /**
     * 查询特定所有者的所有分析结果（分页）
     *
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 分析结果分页结果
     */
    Page<ScriptAnalysisResult> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    /**
     * 查询综合评分高于指定值的分析结果
     *
     * @param minScore 最小评分
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 分析结果分页结果
     */
    @Query("SELECT a FROM ScriptAnalysisResult a WHERE a.overallScore >= :minScore " +
            "AND a.ownerId = :ownerId ORDER BY a.overallScore DESC")
    Page<ScriptAnalysisResult> findByMinScoreAndOwnerId(@Param("minScore") java.math.BigDecimal minScore,
                                                        @Param("ownerId") Long ownerId,
                                                        Pageable pageable);

    /**
     * 查询特定数据来源的分析结果（分页）
     *
     * @param dataSource 数据来源
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 分析结果分页结果
     */
    Page<ScriptAnalysisResult> findByDataSourceAndOwnerIdOrderByCreatedAtDesc(
            String dataSource, Long ownerId, Pageable pageable);
}
