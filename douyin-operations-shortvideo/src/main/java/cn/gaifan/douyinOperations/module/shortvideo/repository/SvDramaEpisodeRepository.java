package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 短剧剧集 Repository (Phase 3)
 */
public interface SvDramaEpisodeRepository extends JpaRepository<SvDramaEpisode, Long> {

    List<SvDramaEpisode> findByDramaIdOrderByEpisodeNumberAsc(Long dramaId);

    java.util.Optional<SvDramaEpisode> findByProjectId(Long projectId);
}
