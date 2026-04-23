package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.KbImportReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbImportReportRepository extends JpaRepository<KbImportReport, Long> {

    List<KbImportReport> findByKbIdAndDeletedOrderByCreateTimeDesc(Long kbId, Integer deleted);
}
