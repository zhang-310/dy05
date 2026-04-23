package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 深度分析结果Repository
 */
@Repository
public interface BenchmarkAnalysisRepository extends JpaRepository<BenchmarkAnalysis, Long>, JpaSpecificationExecutor<BenchmarkAnalysis> {

    /**
     * 根据视频ID查找分析结果
     */
    Optional<BenchmarkAnalysis> findByBenchmarkVideoId(Long benchmarkVideoId);
}
