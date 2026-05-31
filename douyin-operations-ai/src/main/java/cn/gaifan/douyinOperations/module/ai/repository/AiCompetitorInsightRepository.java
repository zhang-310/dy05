package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiCompetitorInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface AiCompetitorInsightRepository extends JpaRepository<AiCompetitorInsight, Long> {
    List<AiCompetitorInsight> findByIngestedToKbAndDeleted(boolean ingested, int deleted);
    List<AiCompetitorInsight> findByCategoryAndDeleted(String category, int deleted);

    @Query("SELECT c FROM AiCompetitorInsight c WHERE c.category = :category AND c.collectedAt >= :since AND c.deleted = 0 ORDER BY c.qualityScore DESC")
    List<AiCompetitorInsight> findRecentByCategory(@Param("category") String category, @Param("since") Timestamp since);

    @Query("SELECT AVG(c.qualityScore) FROM AiCompetitorInsight c WHERE c.category = :category AND c.deleted = 0")
    Double getAvgQualityScore(@Param("category") String category);
}
