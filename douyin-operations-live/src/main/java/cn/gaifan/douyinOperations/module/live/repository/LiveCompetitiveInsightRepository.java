package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveCompetitiveInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LiveCompetitiveInsightRepository extends JpaRepository<LiveCompetitiveInsight, Long>,
        JpaSpecificationExecutor<LiveCompetitiveInsight> {

    Optional<LiveCompetitiveInsight> findByIdAndDeleted(Long id, Integer deleted);
}
