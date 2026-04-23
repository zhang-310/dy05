package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ProductScriptEffectivenessRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 商品话术效果评分历史仓库
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Repository
public interface ProductScriptEffectivenessRecordRepository extends JpaRepository<ProductScriptEffectivenessRecord, Long>,
        JpaSpecificationExecutor<ProductScriptEffectivenessRecord> {

    /**
     * 查询指定版本的评分历史（按计算时间倒序）
     *
     * @param scriptVersionId 话术版本 ID
     * @return 评分历史列表
     */
    List<ProductScriptEffectivenessRecord> findByScriptVersionIdOrderByCalculatedAtDesc(Long scriptVersionId);

    /**
     * 查询指定版本的最新评分记录
     *
     * @param scriptVersionId 话术版本 ID
     * @return 最新评分记录
     */
    @Query(value = """
        SELECT * FROM product_script_effectiveness_record
        WHERE script_version_id = :scriptVersionId AND deleted = 0
        ORDER BY calculated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<ProductScriptEffectivenessRecord> findLatestByScriptVersionId(@Param("scriptVersionId") Long scriptVersionId);

    /**
     * 查询指定产品指定时间范围内的评分记录
     *
     * @param productId 产品 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 评分记录列表
     */
    List<ProductScriptEffectivenessRecord> findByProductIdAndCalculatedAtBetweenOrderByCalculatedAtDesc(
            Long productId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询指定产品的所有评分记录
     *
     * @param productId 产品 ID
     * @return 评分记录列表
     */
    List<ProductScriptEffectivenessRecord> findByProductIdOrderByCalculatedAtDesc(Long productId);

    /**
     * 查询指定产品和所有者的评分记录
     *
     * @param productId 产品 ID
     * @param ownerId 所有者 ID
     * @return 评分记录列表
     */
    List<ProductScriptEffectivenessRecord> findByProductIdAndOwnerIdOrderByCalculatedAtDesc(Long productId, Long ownerId);

    /**
     * 查询指定版本的最新评分值
     *
     * @param scriptVersionId 话术版本 ID
     * @return 最新评分值
     */
    @Query(value = """
        SELECT score_value FROM product_script_effectiveness_record
        WHERE script_version_id = :scriptVersionId AND deleted = 0
        ORDER BY calculated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Double> findLatestScoreByScriptVersionId(@Param("scriptVersionId") Long scriptVersionId);
}
