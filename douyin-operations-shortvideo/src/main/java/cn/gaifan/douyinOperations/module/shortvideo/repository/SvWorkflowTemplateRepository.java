package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 工作流模板 Repository
 */
public interface SvWorkflowTemplateRepository extends JpaRepository<SvWorkflowTemplate, Long> {

    @Query("SELECT w FROM SvWorkflowTemplate w WHERE w.deleted = 0 AND (w.ownerId = :ownerId OR w.ownerId = 0) ORDER BY w.createTime DESC")
    List<SvWorkflowTemplate> findAvailableForOwner(@Param("ownerId") Long ownerId);
}
