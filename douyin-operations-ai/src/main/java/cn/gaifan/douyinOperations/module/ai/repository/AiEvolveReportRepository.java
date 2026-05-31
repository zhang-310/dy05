package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiEvolveReportRepository extends JpaRepository<AiEvolveReport, Long> {

    Optional<AiEvolveReport> findByTaskId(Long taskId);

    List<AiEvolveReport> findByIndexStatusOrderByCreateTimeAsc(String indexStatus, org.springframework.data.domain.Pageable pageable);

    List<AiEvolveReport> findByTaskIdInOrderByCreateTimeDesc(List<Long> taskIds);
}
