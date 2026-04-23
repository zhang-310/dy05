package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScriptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SvScriptTemplateRepository extends JpaRepository<SvScriptTemplate, Long>, JpaSpecificationExecutor<SvScriptTemplate> {

    Optional<SvScriptTemplate> findByIdAndDeleted(Long id, Integer deleted);

    List<SvScriptTemplate> findBySceneAndStatusAndDeleted(String scene, Integer status, Integer deleted);

    List<SvScriptTemplate> findByTemplateTypeAndStatusAndDeleted(String templateType, Integer status, Integer deleted);

    @Modifying
    @Query("UPDATE SvScriptTemplate t SET t.useCount = t.useCount + 1 WHERE t.id = :id")
    void incrementUseCount(@Param("id") Long id);
}
