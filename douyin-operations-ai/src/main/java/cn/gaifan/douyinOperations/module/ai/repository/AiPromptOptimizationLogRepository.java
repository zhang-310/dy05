package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiPromptOptimizationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface AiPromptOptimizationLogRepository extends JpaRepository<AiPromptOptimizationLog, Long> {

    List<AiPromptOptimizationLog> findByTaskTypeAndDeletedOrderByCreateTimeDesc(String taskType, int deleted);

    long countByTaskTypeAndCreateTimeAfterAndDeleted(String taskType, Timestamp since, int deleted);
}
