package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分析任务Repository
 */
@Repository
public interface BenchmarkTaskRepository extends JpaRepository<BenchmarkTask, Long>, JpaSpecificationExecutor<BenchmarkTask> {

    /**
     * 根据ownerId查找所有任务
     */
    List<BenchmarkTask> findByOwnerId(Long ownerId);

    /**
     * 根据ownerId和任务状态查找任务
     */
    List<BenchmarkTask> findByOwnerIdAndTaskStatus(Long ownerId, String taskStatus);

    /**
     * 根据账号ID查找任务
     */
    List<BenchmarkTask> findByBenchmarkAccountId(Long benchmarkAccountId);
}
