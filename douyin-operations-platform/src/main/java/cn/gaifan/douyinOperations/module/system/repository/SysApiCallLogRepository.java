package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.SysApiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface SysApiCallLogRepository extends JpaRepository<SysApiCallLog, Long>, JpaSpecificationExecutor<SysApiCallLog> {

    @Modifying
    @Query("DELETE FROM SysApiCallLog l WHERE l.createTime < :cutoff")
    int deleteByCreateTimeBefore(@Param("cutoff") Timestamp cutoff);

    @Modifying
    @Query(value = "DELETE FROM sys_api_call_log WHERE id IN (SELECT id FROM sys_api_call_log WHERE create_time < :cutoff ORDER BY id LIMIT 1000)", nativeQuery = true)
    int deleteBatchByCreateTimeBefore(@Param("cutoff") Timestamp cutoff);
}
