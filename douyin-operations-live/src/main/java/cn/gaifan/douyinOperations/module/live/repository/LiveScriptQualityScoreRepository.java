package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptQualityScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveScriptQualityScoreRepository extends JpaRepository<LiveScriptQualityScore, Long>,
        JpaSpecificationExecutor<LiveScriptQualityScore> {

    Optional<LiveScriptQualityScore> findTopByScriptIdAndDeletedOrderByCreateTimeDesc(Long scriptId, Integer deleted);

    List<LiveScriptQualityScore> findBySessionIdAndDeletedOrderByTotalQualityScoreDesc(Long sessionId, Integer deleted);

    List<LiveScriptQualityScore> findByScriptIdAndDeletedOrderByCreateTimeDesc(Long scriptId, Integer deleted);
}
