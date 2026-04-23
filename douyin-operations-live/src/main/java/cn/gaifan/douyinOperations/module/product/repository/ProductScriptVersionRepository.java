package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
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
 * 商品话术版本数据访问层
 * 继承 JpaRepository 和 JpaSpecificationExecutor，支持动态查询和分页
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Repository
public interface ProductScriptVersionRepository extends JpaRepository<ProductScriptVersion, Long>,
        JpaSpecificationExecutor<ProductScriptVersion> {

    /**
     * 查询特定产品的所有启用话术版本
     *
     * @param productId 产品 ID
     * @param isActive 是否启用
     * @return 话术版本列表
     */
    List<ProductScriptVersion> findByProductIdAndIsActive(Long productId, Boolean isActive);

    /**
     * 查询特定产品的所有话术版本（包括未删除的）
     *
     * @param productId 产品 ID
     * @param deleted 删除标记
     * @return 话术版本列表
     */
    List<ProductScriptVersion> findByProductIdAndDeleted(Long productId, Integer deleted);

    /**
     * 查询特定产品的启用话术版本，按效果评分降序
     *
     * @param productId 产品 ID
     * @param isActive 是否启用
     * @param pageable 分页参数
     * @return 话术版本分页结果
     */
    Page<ProductScriptVersion> findByProductIdAndIsActive(Long productId, Boolean isActive, Pageable pageable);

    /**
     * 查询特定产品效果最好的话术版本
     *
     * @param productId 产品 ID
     * @return 话术版本，Optional 包装
     */
    @Query("SELECT p FROM ProductScriptVersion p WHERE p.productId = :productId AND p.isActive = true " +
            "ORDER BY p.effectivenessScore DESC LIMIT 1")
    Optional<ProductScriptVersion> findTopByProductIdOrderByEffectivenessScoreDesc(@Param("productId") Long productId);

    /**
     * 查询特定产品和风格的话术版本
     *
     * @param productId 产品 ID
     * @param style 话术风格
     * @param pageable 分页参数
     * @return 话术版本分页结果
     */
    @Query("SELECT p FROM ProductScriptVersion p WHERE p.productId = :productId AND p.style = :style " +
            "AND p.isActive = true ORDER BY p.effectivenessScore DESC")
    Page<ProductScriptVersion> findByProductIdAndStyle(@Param("productId") Long productId,
                                                       @Param("style") String style,
                                                       Pageable pageable);

    /**
     * 查询特定产品是否推荐的话术版本
     *
     * @param productId 产品 ID
     * @param isRecommended 是否推荐
     * @return 话术版本列表
     */
    List<ProductScriptVersion> findByProductIdAndIsRecommended(Long productId, Boolean isRecommended);

    /**
     * 查询特定产品的所有话术版本（不限启用状态）
     *
     * @param productId 产品 ID
     * @return 话术版本列表
     */
    List<ProductScriptVersion> findByProductId(Long productId);

    /**
     * 查询特定产品的话术版本（分页）
     *
     * @param productId 产品 ID
     * @param pageable 分页参数
     * @return 话术版本分页结果
     */
    Page<ProductScriptVersion> findByProductId(Long productId, Pageable pageable);

    /**
     * 查询内容包含关键词的话术版本
     *
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 话术版本分页结果
     */
    @Query("SELECT p FROM ProductScriptVersion p WHERE UPPER(p.content) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
            "AND p.isActive = true ORDER BY p.effectivenessScore DESC")
    Page<ProductScriptVersion> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查询特定产品内容包含关键词的话术版本
     *
     * @param productId 产品 ID
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 话术版本分页结果
     */
    @Query("SELECT p FROM ProductScriptVersion p WHERE p.productId = :productId " +
            "AND UPPER(p.content) LIKE UPPER(CONCAT('%', :keyword, '%')) " +
            "AND p.isActive = true ORDER BY p.effectivenessScore DESC")
    Page<ProductScriptVersion> searchByProductIdAndKeyword(@Param("productId") Long productId,
                                                           @Param("keyword") String keyword,
                                                           Pageable pageable);

    /**
     * 查询特定产品下的最大版本号
     *
     * @param productId 产品 ID
     * @return 最大版本号
     */
    @Query(value = "SELECT COALESCE(MAX(version_number), 0) FROM product_script_version WHERE product_id = :productId AND deleted = 0",
            nativeQuery = true)
    Integer findMaxVersionNumberByProductId(@Param("productId") Long productId);

    /**
     * 查询特定产品和版本号的话术版本
     *
     * @param productId 产品 ID
     * @param versionNumber 版本号
     * @return 话术版本，Optional 包装
     */
    Optional<ProductScriptVersion> findByProductIdAndVersionNumber(Long productId, Integer versionNumber);

    /**
     * 查询特定所有者的话术版本（分页）
     *
     * @param ownerId 所有者 ID
     * @param pageable 分页参数
     * @return 话术版本分页结果
     */
    Page<ProductScriptVersion> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * 查询特定所有者和产品的话术版本（分页）
     *
     * @param ownerId 所有者 ID
     * @param productId 产品 ID
     * @param pageable 分页参数
     * @return 话术版本分页结果
     */
    @Query("SELECT p FROM ProductScriptVersion p WHERE p.ownerId = :ownerId AND p.productId = :productId " +
            "ORDER BY p.versionNumber DESC")
    Page<ProductScriptVersion> findByOwnerIdAndProductId(@Param("ownerId") Long ownerId,
                                                         @Param("productId") Long productId,
                                                         Pageable pageable);
}
