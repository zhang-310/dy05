package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.SysAlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SysAlertRecordRepository extends JpaRepository<SysAlertRecord, Long> {
    Page<SysAlertRecord> findByDeleted(Integer deleted, Pageable pageable);

    @Query("SELECT r.status, COUNT(r) FROM SysAlertRecord r WHERE r.deleted = 0 GROUP BY r.status")
    List<Object[]> countByStatus();

    @Query("SELECT r.severity, COUNT(r) FROM SysAlertRecord r WHERE r.deleted = 0 GROUP BY r.severity")
    List<Object[]> countBySeverity();
}
