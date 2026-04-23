package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCompetitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 竞品 Repository（Phase 4.3）
 */
public interface SvCompetitorRepository extends JpaRepository<SvCompetitor, Long> {

    List<SvCompetitor> findByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, Integer deleted);
}
