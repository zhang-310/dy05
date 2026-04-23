package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvWorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 工作流任务 Repository (P0 持久化)
 */
public interface SvWorkflowTaskRepository extends JpaRepository<SvWorkflowTask, Long> {

    Optional<SvWorkflowTask> findByTaskId(String taskId);
}
