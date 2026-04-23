package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DyProductScriptRepository extends JpaRepository<DyProductScript, Long>, JpaSpecificationExecutor<DyProductScript> {

    /**
     * 查找产品的所有话术
     */
    List<DyProductScript> findByProductIdAndDeletedOrderByCreateTimeDesc(Long productId, Integer deleted);

    /**
     * 查找产品指定类型的话术
     */
    List<DyProductScript> findByProductIdAndScriptTypeAndDeletedOrderByVersionDesc(
            Long productId, String scriptType, Integer deleted);

    /**
     * 查找产品的激活话术
     */
    List<DyProductScript> findByProductIdAndIsActiveAndDeleted(Long productId, Boolean isActive, Integer deleted);

    /**
     * 查找产品指定类型的所有激活话术（v2.0 每风格一个激活，故可能多条）
     */
    List<DyProductScript> findByProductIdAndScriptTypeAndIsActiveAndDeleted(
            Long productId, String scriptType, Boolean isActive, Integer deleted);

    /**
     * 获取产品指定类型的最大版本号
     */
    @Query("SELECT MAX(s.version) FROM DyProductScript s WHERE s.productId = :productId AND s.scriptType = :scriptType AND s.deleted = 0")
    Integer findMaxVersionByProductIdAndScriptType(@Param("productId") Long productId, @Param("scriptType") String scriptType);

    /**
     * 获取产品指定类型+风格的最大版本号（v2.0 按风格维度版本管理）
     * style 为空时匹配 style IS NULL 或 style = ''
     */
    @Query(value = "SELECT MAX(version) FROM dy_product_script WHERE product_id = :productId AND script_type = :scriptType " +
            "AND COALESCE(style, '') = COALESCE(:style, '') AND deleted = 0", nativeQuery = true)
    Integer findMaxVersionByProductIdAndScriptTypeAndStyle(
            @Param("productId") Long productId, @Param("scriptType") String scriptType, @Param("style") String style);

    /**
     * 取消产品指定类型+风格的所有激活状态（v2.0 同风格唯一激活）
     */
    @Modifying
    @Query(value = "UPDATE dy_product_script SET is_active = false WHERE product_id = :productId AND script_type = :scriptType " +
            "AND COALESCE(style, '') = COALESCE(:style, '') AND deleted = 0", nativeQuery = true)
    void deactivateByProductIdAndScriptTypeAndStyle(
            @Param("productId") Long productId, @Param("scriptType") String scriptType, @Param("style") String style);

    /**
     * 取消产品指定类型的所有激活状态
     */
    @Modifying
    @Query("UPDATE DyProductScript s SET s.isActive = false WHERE s.productId = :productId AND s.scriptType = :scriptType AND s.deleted = 0")
    void deactivateAllByProductIdAndScriptType(@Param("productId") Long productId, @Param("scriptType") String scriptType);

    /**
     * 按产品+类型+风格查询话术（版本降序）
     */
    List<DyProductScript> findByProductIdAndScriptTypeAndStyleAndDeletedOrderByVersionDesc(
            Long productId, String scriptType, String style, Integer deleted);

    /**
     * 统计产品话术数量
     */
    long countByProductIdAndDeleted(Long productId, Integer deleted);

    /**
     * 按产品+版本+删除标记查询唯一话术
     */
    Optional<DyProductScript> findByProductIdAndVersionAndDeleted(Long productId, Integer version, Integer deleted);

    /**
     * 按产品+删除标记查询，版本降序
     */
    List<DyProductScript> findByProductIdAndDeletedOrderByVersionDesc(Long productId, Integer deleted);

    /**
     * 按产品+风格+删除标记查询，版本降序
     */
    List<DyProductScript> findByProductIdAndStyleAndDeletedOrderByVersionDesc(Long productId, String style, Integer deleted);

    /**
     * 获取产品下最大版本号（不限 script_type）
     */
    @Query("SELECT MAX(s.version) FROM DyProductScript s WHERE s.productId = :productId AND s.deleted = 0")
    Integer findMaxVersionByProductId(@Param("productId") Long productId);
}
