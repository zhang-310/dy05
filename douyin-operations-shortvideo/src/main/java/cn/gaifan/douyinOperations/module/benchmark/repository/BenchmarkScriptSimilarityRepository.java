package cn.gaifan.douyinOperations.module.benchmark.repository;

import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkScriptSimilarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 脚本相似度索引 Repository
 */
@Repository
public interface BenchmarkScriptSimilarityRepository extends JpaRepository<BenchmarkScriptSimilarity, Long>, JpaSpecificationExecutor<BenchmarkScriptSimilarity> {

    /**
     * 查询指定脚本的相似脚本（按相似度降序）
     */
    List<BenchmarkScriptSimilarity> findBySourceScriptIdAndDeletedOrderBySimilarityScoreDesc(Long sourceScriptId, Integer deleted);

    /**
     * 查询高相似度脚本（相似度 >= 阈值）
     */
    List<BenchmarkScriptSimilarity> findBySourceScriptIdAndSimilarityScoreGreaterThanEqualAndDeletedOrderBySimilarityScoreDesc(
            Long sourceScriptId, BigDecimal minScore, Integer deleted);

    /**
     * 查询指定类型的相似度记录
     */
    List<BenchmarkScriptSimilarity> findBySourceScriptIdAndSimilarityTypeAndDeletedOrderBySimilarityScoreDesc(
            Long sourceScriptId, String similarityType, Integer deleted);

    /**
     * 批量查询相似脚本（用于推荐）
     */
    @Query("SELECT s FROM BenchmarkScriptSimilarity s WHERE s.sourceScriptId IN :scriptIds AND s.similarityScore >= :minScore AND s.deleted = 0 ORDER BY s.similarityScore DESC")
    List<BenchmarkScriptSimilarity> findSimilarScriptsByIds(@Param("scriptIds") List<Long> scriptIds, @Param("minScore") BigDecimal minScore);

    /**
     * 检查是否已存在相似度记录
     */
    boolean existsBySourceScriptIdAndTargetScriptIdAndDeleted(Long sourceScriptId, Long targetScriptId, Integer deleted);
}
