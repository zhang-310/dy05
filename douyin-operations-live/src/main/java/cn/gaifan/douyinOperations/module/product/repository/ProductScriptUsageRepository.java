package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ProductScriptUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 产品话术使用记录 Repository
 */
@Repository
public interface ProductScriptUsageRepository extends JpaRepository<ProductScriptUsage, Long> {

    List<ProductScriptUsage> findByProductScriptIdAndDeletedOrderByAppliedTimeDesc(
            Long productScriptId, Integer deleted
    );

    List<ProductScriptUsage> findByLiveScriptIdAndDeletedOrderByAppliedTimeDesc(
            Long liveScriptId, Integer deleted
    );

    List<ProductScriptUsage> findBySessionIdAndDeletedOrderByAppliedTimeDesc(
            Long sessionId, Integer deleted
    );

    Long countByProductScriptIdAndDeleted(Long productScriptId, Integer deleted);

    @Query("SELECT AVG(p.effectivenessScore) FROM ProductScriptUsage p " +
           "WHERE p.productScriptId = :productScriptId AND p.deleted = 0")
    Double getAverageEffectivenessScore(@Param("productScriptId") Long productScriptId);

    @Query("SELECT AVG(p.conversionRate) FROM ProductScriptUsage p " +
           "WHERE p.productScriptId = :productScriptId AND p.deleted = 0")
    Double getAverageConversionRate(@Param("productScriptId") Long productScriptId);

    @Query("SELECT COALESCE(SUM(p.salesAmount), 0) FROM ProductScriptUsage p " +
           "WHERE p.productScriptId = :productScriptId AND p.deleted = 0")
    java.math.BigDecimal getTotalSalesAmount(@Param("productScriptId") Long productScriptId);

    /** 按直播商品 ID + 话术 ID + deleted 检查是否存在 */
    boolean existsByLiveProductIdAndProductScriptIdAndDeleted(Long liveProductId, Long productScriptId, Integer deleted);

    /** 按场次 ID + 话术 ID + deleted 检查是否存在 */
    boolean existsBySessionIdAndProductScriptIdAndDeleted(Long sessionId, Long productScriptId, Integer deleted);
}
