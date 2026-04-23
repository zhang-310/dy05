package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 话术生成任务 Repository
 */
public interface LiveGenerationTaskRepository extends JpaRepository<LiveGenerationTask, Long> {

    /**
     * 根据场次 ID 和状态列表查询任务
     */
    List<LiveGenerationTask> findBySessionIdAndStatusIn(Long sessionId, List<String> statuses);

    /**
     * 获取场次下最新的生成任务
     */
    LiveGenerationTask findTopBySessionIdOrderByCreateTimeDesc(Long sessionId);
}
