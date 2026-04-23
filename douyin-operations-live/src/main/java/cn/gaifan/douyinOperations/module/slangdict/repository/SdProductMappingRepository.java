package cn.gaifan.douyinOperations.module.slangdict.repository;

import cn.gaifan.douyinOperations.module.slangdict.entity.SdProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SdProductMappingRepository extends JpaRepository<SdProductMapping, Long> {

    List<SdProductMapping> findByProductIdAndUserIdAndDeleted(Long productId, Long userId, Integer deleted);

    List<SdProductMapping> findByEntryIdAndDeleted(Long entryId, Integer deleted);

    @Modifying
    @Query("UPDATE SdProductMapping m SET m.deleted = 1 WHERE m.entryId = :entryId AND m.productId = :productId AND m.userId = :userId AND m.deleted = 0")
    int softDeleteByEntryAndProduct(@Param("entryId") Long entryId, @Param("productId") Long productId, @Param("userId") Long userId);
}
