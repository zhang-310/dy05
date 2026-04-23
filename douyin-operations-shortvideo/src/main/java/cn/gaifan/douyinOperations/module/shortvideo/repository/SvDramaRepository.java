package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDrama;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 短剧 Repository (Phase 3)
 */
public interface SvDramaRepository extends JpaRepository<SvDrama, Long> {

    List<SvDrama> findByOwnerIdOrderByUpdateTimeDesc(Long ownerId);
}
