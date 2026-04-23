package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveCompetitorScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LiveCompetitorScriptRepository extends JpaRepository<LiveCompetitorScript, Long>,
        JpaSpecificationExecutor<LiveCompetitorScript> {

    Optional<LiveCompetitorScript> findByIdAndDeleted(Long id, Integer deleted);
}
