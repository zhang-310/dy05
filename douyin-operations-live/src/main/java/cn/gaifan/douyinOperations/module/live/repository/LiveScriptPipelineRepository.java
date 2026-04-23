package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptPipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 直播话术流水线 Repository
 */
public interface LiveScriptPipelineRepository extends JpaRepository<LiveScriptPipeline, Long>,
        JpaSpecificationExecutor<LiveScriptPipeline> {

    Optional<LiveScriptPipeline> findByIdAndOwnerIdAndDeleted(Long id, Long ownerId, Integer deleted);

    List<LiveScriptPipeline> findBySessionIdAndOwnerIdAndDeleted(Long sessionId, Long ownerId, Integer deleted);

    List<LiveScriptPipeline> findByStatusAndDeleted(String status, Integer deleted);
}
