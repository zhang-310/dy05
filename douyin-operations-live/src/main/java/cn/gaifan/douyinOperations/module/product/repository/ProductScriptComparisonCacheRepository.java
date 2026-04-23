package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ProductScriptComparisonCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 商品话术对比缓存仓库
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Repository
public interface ProductScriptComparisonCacheRepository extends JpaRepository<ProductScriptComparisonCache, Long>,
        JpaSpecificationExecutor<ProductScriptComparisonCache> {

    /**
     * 查询指定产品和对比类型的缓存
     *
     * @param productId 产品 ID
     * @param comparisonType 对比类型
     * @return 缓存数据（可能为空）
     */
    Optional<ProductScriptComparisonCache> findByProductIdAndComparisonTypeAndDeleted(
            Long productId, String comparisonType, Integer deleted);

    /**
     * 查询指定产品、对比类型和所有者的缓存
     *
     * @param productId 产品 ID
     * @param comparisonType 对比类型
     * @param ownerId 所有者 ID
     * @return 缓存数据（可能为空）
     */
    Optional<ProductScriptComparisonCache> findByProductIdAndComparisonTypeAndOwnerIdAndDeleted(
            Long productId, String comparisonType, Long ownerId, Integer deleted);

    /**
     * 查询指定产品的所有缓存
     *
     * @param productId 产品 ID
     * @return 缓存列表
     */
    List<ProductScriptComparisonCache> findByProductIdAndDeletedOrderByCachedAtDesc(Long productId, Integer deleted);

    /**
     * 删除指定产品和对比类型的缓存（逻辑删除）
     *
     * @param productId 产品 ID
     * @param comparisonType 对比类型
     * @return 删除的行数
     */
    @Modifying
    @Query(value = """
        UPDATE product_script_comparison_cache
        SET deleted = 1
        WHERE product_id = :productId AND comparison_type = :comparisonType AND deleted = 0
        """, nativeQuery = true)
    int deleteByProductIdAndComparisonType(@Param("productId") Long productId,
                                           @Param("comparisonType") String comparisonType);

    /**
     * 删除指定产品的所有缓存（逻辑删除）
     *
     * @param productId 产品 ID
     * @return 删除的行数
     */
    @Modifying
    @Query(value = """
        UPDATE product_script_comparison_cache
        SET deleted = 1
        WHERE product_id = :productId AND deleted = 0
        """, nativeQuery = true)
    int deleteByProductId(@Param("productId") Long productId);
}
