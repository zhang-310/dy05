package cn.gaifan.douyinOperations.module.product.repository;

import cn.gaifan.douyinOperations.module.product.entity.StyleRecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 风格推荐反馈 Repository
 *
 * @author Claude Code
 * @since 2026-04-04
 */
@Repository
public interface StyleRecommendationFeedbackRepository extends JpaRepository<StyleRecommendationFeedback, Long>,
        JpaSpecificationExecutor<StyleRecommendationFeedback> {

    /**
     * 查找商品的所有反馈
     */
    List<StyleRecommendationFeedback> findByProductIdAndDeleted(Long productId, Integer deleted);

    /**
     * 查找用户的所有反馈
     */
    List<StyleRecommendationFeedback> findByUserIdAndDeletedOrderByCreatedAtDesc(Long userId, Integer deleted);

    /**
     * 统计指定时间范围内的Top-3命中率
     */
    @Query("SELECT AVG(CASE WHEN f.isTop3Hit = true THEN 1.0 ELSE 0.0 END) " +
           "FROM StyleRecommendationFeedback f " +
           "WHERE f.userId = :userId AND f.deleted = 0 " +
           "AND f.createdAt >= :startTime AND f.createdAt <= :endTime")
    Double calculateTop3HitRate(@Param("userId") Long userId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定推荐源的反馈数量
     */
    Long countByRecommendationSourceAndUserIdAndDeleted(String recommendationSource, Long userId, Integer deleted);
}
