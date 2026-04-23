package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvOpsReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SvOpsReportTemplateRepository extends JpaRepository<SvOpsReportTemplate, Long> {

    Optional<SvOpsReportTemplate> findByIdAndOwnerIdAndDeleted(Long id, Long ownerId, Integer deleted);

    List<SvOpsReportTemplate> findByOwnerIdAndDeletedOrderByUpdateTimeDesc(Long ownerId, Integer deleted);
}
