package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface LiveScriptApprovalRepository extends JpaRepository<LiveScriptApproval, Long>,
        JpaSpecificationExecutor<LiveScriptApproval> {

    Optional<LiveScriptApproval> findByIdAndDeleted(Long id, Integer deleted);

    Page<LiveScriptApproval> findBySessionIdAndDeleted(Long sessionId, Integer deleted, Pageable pageable);

    List<LiveScriptApproval> findByScriptIdAndDeleted(Long scriptId, Integer deleted);

    /** 最新一条审核记录 */
    Optional<LiveScriptApproval> findTopByScriptIdAndDeletedOrderByCreateTimeDesc(Long scriptId, Integer deleted);

    /** 待审核列表 */
    Page<LiveScriptApproval> findByStatusAndDeleted(Integer status, Integer deleted, Pageable pageable);
}
