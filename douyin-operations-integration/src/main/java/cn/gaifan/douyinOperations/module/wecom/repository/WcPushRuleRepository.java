package cn.gaifan.douyinOperations.module.wecom.repository;

import cn.gaifan.douyinOperations.module.wecom.entity.WcPushRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface WcPushRuleRepository extends JpaRepository<WcPushRule, Long>, JpaSpecificationExecutor<WcPushRule> {

    Optional<WcPushRule> findByIdAndDeleted(Long id, Integer deleted);

    List<WcPushRule> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);

    @Modifying
    @Query("UPDATE WcPushRule r SET r.status = :status WHERE r.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Modifying
    @Query("UPDATE WcPushRule r SET r.lastTriggerTime = :time WHERE r.id = :id")
    void updateLastTriggerTime(@Param("id") Long id, @Param("time") Timestamp time);
}
