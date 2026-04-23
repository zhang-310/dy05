package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 直播审批日志 Repository
 */
public interface LiveApprovalLogRepository extends JpaRepository<LiveApprovalLog, Long> {

    List<LiveApprovalLog> findBySessionIdAndDeletedOrderByCreateTimeDesc(Long sessionId, int deleted);

    List<LiveApprovalLog> findByScriptIdAndDeletedOrderByCreateTimeDesc(Long scriptId, int deleted);
}
