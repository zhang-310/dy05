package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCompetitorSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 竞品快照 Repository（Phase 4.3）
 */
public interface SvCompetitorSnapshotRepository extends JpaRepository<SvCompetitorSnapshot, Long> {

    Optional<SvCompetitorSnapshot> findByCompetitorIdAndSnapshotDate(Long competitorId, LocalDate snapshotDate);
}
