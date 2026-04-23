package cn.gaifan.douyinOperations.module.abtest.repository;

import cn.gaifan.douyinOperations.module.abtest.entity.AbVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AbVariantRepository extends JpaRepository<AbVariant, Long>, JpaSpecificationExecutor<AbVariant> {

    Optional<AbVariant> findByIdAndDeleted(Long id, Integer deleted);

    List<AbVariant> findByExperimentIdAndDeleted(Long experimentId, Integer deleted);

    @Modifying
    @Query("UPDATE AbVariant v SET v.viewCount = v.viewCount + 1, v.conversionRate = CAST((v.conversionCount * 1.0 / NULLIF(v.viewCount + 1, 0)) AS java.math.BigDecimal) WHERE v.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AbVariant v SET v.clickCount = v.clickCount + 1 WHERE v.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AbVariant v SET v.conversionCount = v.conversionCount + 1, v.conversionRate = CAST(((v.conversionCount + 1) * 1.0 / NULLIF(v.viewCount, 0)) AS java.math.BigDecimal) WHERE v.id = :id")
    void incrementConversionCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AbVariant v SET v.isWinner = :isWinner WHERE v.id = :id")
    void updateIsWinner(@Param("id") Long id, @Param("isWinner") Integer isWinner);
}
