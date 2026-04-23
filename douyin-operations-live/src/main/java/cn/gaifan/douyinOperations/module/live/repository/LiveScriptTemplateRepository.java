package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 直播话术模板 Repository
 */
public interface LiveScriptTemplateRepository extends JpaRepository<LiveScriptTemplate, Long>, JpaSpecificationExecutor<LiveScriptTemplate> {

    List<LiveScriptTemplate> findByScriptTypeAndDeletedOrderByEffectivenessScoreDesc(String scriptType, Integer deleted, Pageable pageable);

    @Query("SELECT t FROM LiveScriptTemplate t WHERE t.deleted = 0 AND t.sourceScriptId = :scriptId")
    List<LiveScriptTemplate> findBySourceScriptId(@Param("scriptId") Long scriptId);
}
