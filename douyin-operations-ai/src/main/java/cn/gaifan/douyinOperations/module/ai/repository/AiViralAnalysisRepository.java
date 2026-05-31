package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiViralAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiViralAnalysisRepository extends JpaRepository<AiViralAnalysis, Long>, JpaSpecificationExecutor<AiViralAnalysis> {

    Optional<AiViralAnalysis> findByIdAndDeleted(Long id, Integer deleted);

    Optional<AiViralAnalysis> findByVideoIdAndDeleted(Long videoId, Integer deleted);

    @Modifying
    @Query("UPDATE AiViralAnalysis a SET a.status = :status, a.reportContent = :report, a.successFactors = :factors, a.replicableMethods = :methods, a.qualityScore = :quality, a.viralScore = :quality, a.tokensUsed = :tokens, a.modelUsed = :model WHERE a.id = :id")
    void completeAnalysis(@Param("id") Long id, @Param("status") Integer status,
                          @Param("report") String report, @Param("factors") String factors,
                          @Param("methods") String methods, @Param("quality") Integer quality,
                          @Param("tokens") Long tokens, @Param("model") String model);
}
