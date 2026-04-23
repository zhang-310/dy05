package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoGenerationTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 图生视频异步任务 Repository (Phase 2.2)
 */
public interface SvVideoGenerationTaskRepository extends JpaRepository<SvVideoGenerationTask, Long> {

    Page<SvVideoGenerationTask> findByOwnerIdOrderByCreateTimeDesc(Long ownerId, Pageable pageable);

    Page<SvVideoGenerationTask> findByOwnerIdAndProjectIdOrderByCreateTimeDesc(Long ownerId, Long projectId, Pageable pageable);

    /** 按状态 + 处理开始时间查询（超时检测用） */
    @org.springframework.data.jpa.repository.Query("SELECT t FROM SvVideoGenerationTask t WHERE t.status = :status AND t.processingStartedAt < :before")
    List<SvVideoGenerationTask> findByStatusAndProcessingStartedAtBefore(@org.springframework.data.repository.query.Param("status") String status, @org.springframework.data.repository.query.Param("before") java.sql.Timestamp before);
}
