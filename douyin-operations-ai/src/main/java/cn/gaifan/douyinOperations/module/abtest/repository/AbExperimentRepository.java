package cn.gaifan.douyinOperations.module.abtest.repository;

import cn.gaifan.douyinOperations.module.abtest.entity.AbExperiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface AbExperimentRepository extends JpaRepository<AbExperiment, Long>, JpaSpecificationExecutor<AbExperiment> {

    Optional<AbExperiment> findByIdAndDeleted(Long id, Integer deleted);

    /** 话术风格实验：按目标实体查运行中的实验 */
    List<AbExperiment> findByExperimentTypeAndTargetEntityTypeAndTargetEntityIdAndStatusAndDeleted(
            String experimentType, String targetEntityType, Long targetEntityId, Integer status, Integer deleted);

    @Modifying
    @Query("UPDATE AbExperiment e SET e.status = :status, e.startTime = CASE WHEN :status = 1 THEN :now ELSE e.startTime END, e.endTime = CASE WHEN :status = 2 THEN :now ELSE e.endTime END WHERE e.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("now") Timestamp now);

    @Modifying
    @Query("UPDATE AbExperiment e SET e.winnerVariantId = :variantId, e.conclusion = :conclusion, e.status = 2, e.endTime = :now WHERE e.id = :id")
    void setWinner(@Param("id") Long id, @Param("variantId") Long variantId, @Param("conclusion") String conclusion, @Param("now") Timestamp now);

    /** 按状态和逻辑删除查询 */
    List<AbExperiment> findByStatusAndDeleted(Integer status, Integer deleted);
}
