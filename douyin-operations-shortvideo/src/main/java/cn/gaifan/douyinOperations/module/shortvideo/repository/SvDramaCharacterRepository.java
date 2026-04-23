package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 短剧角色 Repository (Phase 3)
 */
public interface SvDramaCharacterRepository extends JpaRepository<SvDramaCharacter, Long> {

    List<SvDramaCharacter> findByDramaIdOrderByIdAsc(Long dramaId);
}
