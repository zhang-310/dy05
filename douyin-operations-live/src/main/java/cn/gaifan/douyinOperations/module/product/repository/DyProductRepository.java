package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DyProductRepository extends JpaRepository<DyProduct, Long>, JpaSpecificationExecutor<DyProduct> {

    Optional<DyProduct> findByIdAndDeleted(Long id, Integer deleted);

    List<DyProduct> findByUserIdAndDeleted(Long userId, Integer deleted);

    /**
     * 锁定商品行，用于话术版本号并发生成时串行化（避免版本号冲突）
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM DyProduct p WHERE p.id = :id AND p.deleted = 0")
    Optional<DyProduct> findByIdForUpdate(@Param("id") Long id);

    Optional<DyProduct> findByUserIdAndSkuAndDeleted(Long userId, String sku, Integer deleted);


    @Modifying
    @Query("UPDATE DyProduct p SET p.status = :status WHERE p.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Modifying
    @Query("UPDATE DyProduct p SET p.featured = :featured WHERE p.id = :id")
    void updateFeatured(@Param("id") Long id, @Param("featured") Integer featured);
}
