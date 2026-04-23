package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.SysSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface SysSyncLogRepository extends JpaRepository<SysSyncLog, Long>, JpaSpecificationExecutor<SysSyncLog> {

    @Modifying
    @Query("UPDATE SysSyncLog l SET l.totalCount = :total, l.successCount = :success, l.failCount = :fail WHERE l.id = :id")
    void updateProgress(@Param("id") Long id, @Param("total") int total,
                        @Param("success") int success, @Param("fail") int fail);

    @Modifying
    @Query("UPDATE SysSyncLog l SET l.status = :status, l.totalCount = :total, l.successCount = :success, l.failCount = :fail, l.endTime = :endTime WHERE l.id = :id")
    void completeSync(@Param("id") Long id, @Param("status") String status,
                      @Param("total") int total, @Param("success") int success,
                      @Param("fail") int fail, @Param("endTime") Timestamp endTime);

    @Modifying
    @Query("UPDATE SysSyncLog l SET l.status = :status, l.errorMessage = :errorMessage, l.endTime = :endTime WHERE l.id = :id")
    void failSync(@Param("id") Long id, @Param("status") String status,
                  @Param("errorMessage") String errorMessage, @Param("endTime") Timestamp endTime);
}
