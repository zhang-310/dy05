package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvStoryFormulaTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SvStoryFormulaTemplateRepository extends JpaRepository<SvStoryFormulaTemplate, Long> {

    Optional<SvStoryFormulaTemplate> findByIdAndOwnerIdAndDeleted(Long id, Long ownerId, Integer deleted);

    List<SvStoryFormulaTemplate> findByOwnerIdAndDeletedOrderByUpdateTimeDesc(Long ownerId, Integer deleted);
}
