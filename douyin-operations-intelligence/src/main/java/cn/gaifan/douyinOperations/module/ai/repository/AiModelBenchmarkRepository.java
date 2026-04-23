package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiModelBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiModelBenchmarkRepository extends JpaRepository<AiModelBenchmark, Long> {

    List<AiModelBenchmark> findByTaskCode(String taskCode);

    @Query("SELECT b.modelId, b.taskCode, AVG(b.latencyMs), " +
            "SUM(CASE WHEN b.success = true THEN 1 ELSE 0 END) * 1.0 / COUNT(b), " +
            "COALESCE(AVG(b.tokensUsed), 0), COUNT(b) " +
            "FROM AiModelBenchmark b WHERE b.taskCode = :taskCode " +
            "GROUP BY b.modelId, b.taskCode ORDER BY b.modelId ASC")
    List<Object[]> aggregateByTaskCode(@Param("taskCode") String taskCode);

    /** 全部任务：按 (modelId, taskCode) 聚合 */
    @Query("SELECT b.modelId, b.taskCode, AVG(b.latencyMs), " +
            "SUM(CASE WHEN b.success = true THEN 1 ELSE 0 END) * 1.0 / COUNT(b), " +
            "COALESCE(AVG(b.tokensUsed), 0), COUNT(b) " +
            "FROM AiModelBenchmark b GROUP BY b.modelId, b.taskCode " +
            "ORDER BY b.taskCode ASC, b.modelId ASC")
    List<Object[]> aggregateAllByModelAndTask();
}
