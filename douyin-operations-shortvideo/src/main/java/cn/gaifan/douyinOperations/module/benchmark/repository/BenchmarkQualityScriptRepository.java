package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkQualityScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 质量脚本知识库 Repository
 */
@Repository
public interface BenchmarkQualityScriptRepository extends JpaRepository<BenchmarkQualityScript, Long>, JpaSpecificationExecutor<BenchmarkQualityScript> {

    /**
     * 根据视频 ID 查询
     */
    Optional<BenchmarkQualityScript> findByVideoIdAndDeleted(Long videoId, Integer deleted);

    /**
     * 根据分析 ID 查询
     */
    Optional<BenchmarkQualityScript> findByAnalysisIdAndDeleted(Long analysisId, Integer deleted);

    /**
     * 查询指定行业的高质量脚本
     */
    List<BenchmarkQualityScript> findByOwnerIdAndIndustryAndQualityScoreGreaterThanEqualAndDeletedOrderByQualityScoreDesc(
            Long ownerId, String industry, BigDecimal minScore, Integer deleted);

    /**
     * 查询指定场景类型的高质量脚本
     */
    List<BenchmarkQualityScript> findByOwnerIdAndSceneTypeAndQualityScoreGreaterThanEqualAndDeletedOrderByQualityScoreDesc(
            Long ownerId, String sceneType, BigDecimal minScore, Integer deleted);

    /**
     * 查询互动率最高的脚本
     */
    List<BenchmarkQualityScript> findByOwnerIdAndDeletedOrderByEngagementRateDesc(Long ownerId, Integer deleted);

    /**
     * 查询传播力最高的脚本
     */
    List<BenchmarkQualityScript> findByOwnerIdAndDeletedOrderByViralScoreDesc(Long ownerId, Integer deleted);

    /**
     * 统计指定行业的脚本数量
     */
    @Query("SELECT COUNT(s) FROM BenchmarkQualityScript s WHERE s.ownerId = :ownerId AND s.industry = :industry AND s.deleted = 0")
    Long countByIndustry(@Param("ownerId") Long ownerId, @Param("industry") String industry);

    /**
     * 统计高质量脚本数量（质量分 >= 阈值）
     */
    @Query("SELECT COUNT(s) FROM BenchmarkQualityScript s WHERE s.ownerId = :ownerId AND s.qualityScore >= :minScore AND s.deleted = 0")
    Long countHighQualityScripts(@Param("ownerId") Long ownerId, @Param("minScore") BigDecimal minScore);
}
