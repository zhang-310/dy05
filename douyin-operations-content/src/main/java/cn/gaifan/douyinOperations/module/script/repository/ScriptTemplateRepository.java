package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ScriptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScriptTemplateRepository extends JpaRepository<ScriptTemplate, Long>, JpaSpecificationExecutor<ScriptTemplate> {

    Optional<ScriptTemplate> findByIdAndDeleted(Long id, Integer deleted);

    List<ScriptTemplate> findByTemplateTypeAndStatusAndDeleted(String templateType, Integer status, Integer deleted);

    List<ScriptTemplate> findBySceneAndStatusAndDeleted(String scene, Integer status, Integer deleted);

    @Modifying
    @Query("UPDATE ScriptTemplate t SET t.useCount = t.useCount + 1 WHERE t.id = :id")
    void incrementUseCount(@Param("id") Long id);
}
