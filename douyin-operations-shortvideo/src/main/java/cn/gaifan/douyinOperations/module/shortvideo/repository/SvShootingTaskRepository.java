package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShootingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.sql.Date;

public interface SvShootingTaskRepository extends JpaRepository<SvShootingTask, Long>, JpaSpecificationExecutor<SvShootingTask> {

    /**
     * 幂等：同一用户、人设、拍摄日已存在带标记说明的调度工单
     */
    boolean existsByOwnerIdAndPersonaIdAndShootDateAndDescriptionContainingAndDeleted(
            Long ownerId, Long personaId, Date shootDate, String marker, Integer deleted);
}
