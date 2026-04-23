package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkScriptUsageEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 脚本使用效果 Repository
 */
@Repository
public interface BenchmarkScriptUsageEffectRepository extends JpaRepository<BenchmarkScriptUsageEffect, Long>, JpaSpecificationExecutor<BenchmarkScriptUsageEffect> {

    /**
     * 查询指定脚本的使用效果记录
     */
    List<BenchmarkScriptUsageEffect> findByQualityScriptIdAndDeletedOrderByCreateTimeDesc(Long qualityScriptId, Integer deleted);

    /**
     * 查询指定视频的使用效果记录
     */
    List<BenchmarkScriptUsageEffect> findByUsageVideoIdAndDeleted(Long usageVideoId, Integer deleted);

    /**
     * 统计脚本的平均效果评分
     */
    @Query("SELECT AVG(e.effectRating) FROM BenchmarkScriptUsageEffect e WHERE e.qualityScriptId = :scriptId AND e.effectRating IS NOT NULL AND e.deleted = 0")
    Double calculateAvgEffectRating(@Param("scriptId") Long scriptId);

    /**
     * 统计脚本的使用次数
     */
    @Query("SELECT COUNT(e) FROM BenchmarkScriptUsageEffect e WHERE e.qualityScriptId = :scriptId AND e.deleted = 0")
    Long countUsageByScriptId(@Param("scriptId") Long scriptId);

    /**
     * 查询高评分的使用效果记录
     */
    List<BenchmarkScriptUsageEffect> findByQualityScriptIdAndEffectRatingGreaterThanEqualAndDeletedOrderByEffectRatingDesc(
            Long qualityScriptId, Integer minRating, Integer deleted);
}
