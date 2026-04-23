package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface AiEvolveTaskRepository extends JpaRepository<AiEvolveTask, Long>, JpaSpecificationExecutor<AiEvolveTask> {

    Optional<AiEvolveTask> findByTaskNo(String taskNo);

    List<AiEvolveTask> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);

    List<AiEvolveTask> findTop10ByOrderByCreateTimeDesc();

    /** 按 kbId + status 查询（DAG blocked 解锁用） */
    List<AiEvolveTask> findByKbIdAndStatusOrderByCreateTimeAsc(Long kbId, String status);

    /** 批量按 taskNo 查询 */
    List<AiEvolveTask> findByTaskNoIn(List<String> taskNos);

    /** 查询带依赖信息的任务（DAG 环检测用） */
    @Query("SELECT t FROM AiEvolveTask t WHERE t.kbId = :kbId AND t.dependsOnTaskNos IS NOT NULL")
    List<AiEvolveTask> findWithDependenciesByKbId(@Param("kbId") Long kbId);

    /** 近 N 天有评分的任务（用于趋势） */
    @Query("SELECT t FROM AiEvolveTask t WHERE t.createTime >= :since AND t.scoreTotal IS NOT NULL ORDER BY t.createTime ASC")
    List<AiEvolveTask> findScoredTasksSince(@Param("since") Timestamp since);

    /** 高分任务（用于进化感知归因：这些任务的主题有实战价值） */
    @Query("SELECT t FROM AiEvolveTask t WHERE t.createTime >= :since AND t.scoreTotal >= :minScore AND t.topicIds IS NOT NULL")
    List<AiEvolveTask> findHighScoreTasksSince(@Param("since") Timestamp since, @Param("minScore") int minScore);

    /** 指定 kb 的高分任务 */
    @Query("SELECT t FROM AiEvolveTask t WHERE t.kbId = :kbId AND t.createTime >= :since AND t.scoreTotal >= :minScore AND t.topicIds IS NOT NULL")
    List<AiEvolveTask> findHighScoreTasksSinceByKbId(@Param("kbId") Long kbId, @Param("since") Timestamp since, @Param("minScore") int minScore);

    /** 最近 N 个已完成任务（用于方法论去重黑名单） */
    @Query("SELECT t FROM AiEvolveTask t WHERE t.kbId = :kbId AND t.status = 'completed' AND t.scoreTotal >= 50 ORDER BY t.createTime DESC")
    List<AiEvolveTask> findRecentCompletedByKbId(@Param("kbId") Long kbId, Pageable pageable);

    /** 按 kbId 统计任务数（round_robin 角度选择用） */
    long countByKbId(Long kbId);

    /** 最近 N 个任务的进化角度（least_used 角度选择用） */
    @Query("SELECT t.evolveAngle FROM AiEvolveTask t WHERE t.kbId = :kbId AND t.evolveAngle IS NOT NULL ORDER BY t.createTime DESC")
    List<String> findRecentEvolveAnglesByKbId(@Param("kbId") Long kbId, Pageable pageable);

    /** 统计指定时间区间内完成的任务数（进化速度统计） */
    @Query("SELECT COUNT(t) FROM AiEvolveTask t WHERE t.status = 'completed' AND t.createTime >= :start AND t.createTime < :end")
    long countCompletedBetween(@Param("start") Timestamp start, @Param("end") Timestamp end);
}
