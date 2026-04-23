package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCinematicPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 运镜 Prompt 知识库 Repository (Phase 5)
 */
public interface SvCinematicPresetRepository extends JpaRepository<SvCinematicPreset, Long> {

    List<SvCinematicPreset> findByCameraTypeAndDeletedOrderBySuccessRateDesc(String cameraType, Integer deleted);

    List<SvCinematicPreset> findByOwnerIdAndDeletedOrderBySuccessRateDesc(Long ownerId, Integer deleted);

    Optional<SvCinematicPreset> findByIdAndDeleted(Long id, Integer deleted);

    /** 更新成功率与使用次数（反馈闭环） */
    @Modifying
    @Query("UPDATE SvCinematicPreset p SET p.successRate = :successRate, p.avgQualityScore = :avgScore, p.useCount = p.useCount + 1 WHERE p.id = :id")
    void updateSuccessStats(@Param("id") Long id, @Param("successRate") BigDecimal successRate, @Param("avgScore") BigDecimal avgScore);
}
