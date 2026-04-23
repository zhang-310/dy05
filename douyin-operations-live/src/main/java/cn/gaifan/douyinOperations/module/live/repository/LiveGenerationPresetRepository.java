package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 生成配置预设 Repository
 */
public interface LiveGenerationPresetRepository extends JpaRepository<LiveGenerationPreset, Long>, JpaSpecificationExecutor<LiveGenerationPreset> {

    List<LiveGenerationPreset> findByOwnerIdOrderByCreateTimeDesc(Long ownerId);

    Optional<LiveGenerationPreset> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<LiveGenerationPreset> findByOwnerIdAndIsDefaultTrue(Long ownerId);

    @Modifying
    @Query("UPDATE LiveGenerationPreset p SET p.isDefault = false, p.updateTime = CURRENT_TIMESTAMP " +
            "WHERE p.ownerId = :ownerId AND p.isDefault = true AND p.deleted = 0")
    int clearDefaultsByOwnerId(@Param("ownerId") Long ownerId);
}
