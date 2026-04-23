package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvRemakeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * 二创模板 Repository（Phase 4.1）
 */
public interface SvRemakeTemplateRepository extends JpaRepository<SvRemakeTemplate, Long>, JpaSpecificationExecutor<SvRemakeTemplate> {

    List<SvRemakeTemplate> findByRemakeTypeAndDeleted(String remakeType, Integer deleted);

    List<SvRemakeTemplate> findByOwnerIdInAndDeleted(List<Long> ownerIds, Integer deleted);
}
