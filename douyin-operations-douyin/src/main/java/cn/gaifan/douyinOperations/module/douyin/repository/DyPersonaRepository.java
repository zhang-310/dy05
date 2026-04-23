package cn.gaifan.douyinOperations.module.douyin.repository;

import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DyPersonaRepository extends JpaRepository<DyPersona, Long>, JpaSpecificationExecutor<DyPersona> {

    Optional<DyPersona> findByIdAndDeleted(Long id, Integer deleted);

    List<DyPersona> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    List<DyPersona> findByOwnerIdAndStatusAndDeleted(Long ownerId, Integer status, Integer deleted);

    List<DyPersona> findByOwnerIdAndPersonaTypeAndDeleted(Long ownerId, String personaType, Integer deleted);

    Optional<DyPersona> findByOwnerIdAndIsDefaultAndDeleted(Long ownerId, Integer isDefault, Integer deleted);

    Optional<DyPersona> findByAccountIdAndOwnerIdAndDeleted(Long accountId, Long ownerId, Integer deleted);

    /**
     * 清除用户的所有默认人设
     */
    @Modifying
    @Query("UPDATE DyPersona p SET p.isDefault = 0, p.updateTime = CURRENT_TIMESTAMP " +
           "WHERE p.ownerId = :ownerId AND p.deleted = 0")
    int clearDefaultByOwnerId(@Param("ownerId") Long ownerId);
}
