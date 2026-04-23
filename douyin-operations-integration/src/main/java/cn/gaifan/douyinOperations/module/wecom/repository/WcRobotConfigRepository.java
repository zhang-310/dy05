package cn.gaifan.douyinOperations.module.wecom.repository;

import cn.gaifan.douyinOperations.module.wecom.entity.WcRobotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WcRobotConfigRepository extends JpaRepository<WcRobotConfig, Long>, JpaSpecificationExecutor<WcRobotConfig> {

    Optional<WcRobotConfig> findByIdAndDeleted(Long id, Integer deleted);

    List<WcRobotConfig> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    @Modifying
    @Query("UPDATE WcRobotConfig r SET r.status = :status WHERE r.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
