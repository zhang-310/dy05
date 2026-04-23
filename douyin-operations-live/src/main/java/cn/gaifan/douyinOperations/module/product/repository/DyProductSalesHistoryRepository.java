package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.DyProductSalesHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface DyProductSalesHistoryRepository extends JpaRepository<DyProductSalesHistory, Long>, JpaSpecificationExecutor<DyProductSalesHistory> {

    Optional<DyProductSalesHistory> findByIdAndDeleted(Long id, Integer deleted);

    @Query("SELECT COALESCE(SUM(h.saleAmount), 0) FROM DyProductSalesHistory h WHERE h.productId = :productId AND h.deleted = 0")
    BigDecimal sumSaleAmountByProductId(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(h.saleQuantity), 0) FROM DyProductSalesHistory h WHERE h.productId = :productId AND h.deleted = 0")
    Long sumSaleQuantityByProductId(@Param("productId") Long productId);
}
