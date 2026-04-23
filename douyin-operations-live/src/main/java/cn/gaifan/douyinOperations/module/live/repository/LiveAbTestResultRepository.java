package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveAbTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/**
 * A/B 测试结果 Repository
 */
public interface LiveAbTestResultRepository extends JpaRepository<LiveAbTestResult, Long>,
        JpaSpecificationExecutor<LiveAbTestResult> {

    List<LiveAbTestResult> findByExperimentKeyAndDeletedOrderByEffectivenessScoreDesc(
            String experimentKey, Integer deleted);

    List<LiveAbTestResult> findBySessionIdAndOwnerIdAndDeleted(Long sessionId, Long ownerId, Integer deleted);

    List<LiveAbTestResult> findByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, Integer deleted);
}
