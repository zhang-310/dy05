package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolvePendingDeepen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiEvolvePendingDeepenRepository extends JpaRepository<AiEvolvePendingDeepen, Long> {

    List<AiEvolvePendingDeepen> findByStatusOrderByPriorityLevelAscCreateTimeAsc(String status, org.springframework.data.domain.Pageable pageable);

    /** 按 kbId + status + deleted 分页查询（主题扩展 Agent 用） */
    List<AiEvolvePendingDeepen> findByKbIdAndStatusAndDeletedOrderByPriorityLevelAscCreateTimeAsc(Long kbId, String status, Integer deleted, org.springframework.data.domain.Pageable pageable);

    List<AiEvolvePendingDeepen> findByTaskId(Long taskId);

    long countByStatus(String status);

    Page<AiEvolvePendingDeepen> findByDeletedOrderByPriorityLevelAscCreateTimeAsc(Integer deleted, Pageable pageable);

    Page<AiEvolvePendingDeepen> findByDeletedAndStatusOrderByPriorityLevelAscCreateTimeAsc(
            Integer deleted, String status, Pageable pageable);
}
