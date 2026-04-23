package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveLearningMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LiveLearningMemoryRepository extends JpaRepository<LiveLearningMemory, Long> {

    List<LiveLearningMemory> findByUserIdAndCategoryAndDeletedOrderByConfidenceDesc(
            Long userId, String category, int deleted);

    List<LiveLearningMemory> findTop5ByUserIdAndCategoryAndConfidenceGreaterThanAndDeletedOrderByConfidenceDesc(
            Long userId, String category, BigDecimal minConfidence, int deleted);

    @Modifying
    @Query("UPDATE LiveLearningMemory m SET m.usageCount = m.usageCount + 1, m.lastUsedAt = CURRENT_TIMESTAMP WHERE m.id = :id")
    void incrementUsageCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE LiveLearningMemory m SET m.confidence = m.confidence * 0.9 WHERE m.id = :id")
    void decayConfidence(@Param("id") Long id);

    List<LiveLearningMemory> findByConfidenceLessThanAndDeleted(BigDecimal threshold, int deleted);
}
