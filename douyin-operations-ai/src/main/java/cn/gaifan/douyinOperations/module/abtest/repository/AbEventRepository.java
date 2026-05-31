package cn.gaifan.douyinOperations.module.abtest.repository;

import cn.gaifan.douyinOperations.module.abtest.entity.AbEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface AbEventRepository extends JpaRepository<AbEvent, Long>, JpaSpecificationExecutor<AbEvent> {

    boolean existsByVariantIdAndEventTypeAndUserFingerprint(Long variantId, String eventType, String userFingerprint);

    @Query("SELECT COUNT(e) FROM AbEvent e WHERE e.experimentId = :experimentId AND e.eventType = :eventType")
    long countByExperimentIdAndEventType(@Param("experimentId") Long experimentId, @Param("eventType") String eventType);

    /**
     * 获取变体统计数据：浏览量、转化量、转化率
     */
    @Query(value = """
            SELECT
                v.id as variant_id,
                v.name as variant_name,
                v.variant_type as variant_type,
                COUNT(CASE WHEN e.event_type = 'view' THEN 1 END) as view_count,
                COUNT(CASE WHEN e.event_type = 'conversion' THEN 1 END) as conversion_count,
                ROUND(100.0 * COUNT(CASE WHEN e.event_type = 'conversion' THEN 1 END) /
                      NULLIF(COUNT(CASE WHEN e.event_type = 'view' THEN 1 END), 0), 2) as conversion_rate,
                ROUND(AVG(CASE WHEN e.event_type = 'conversion' THEN e.value ELSE NULL END)::numeric, 2) as avg_value
            FROM ab_event e
            JOIN ab_variant v ON e.variant_id = v.id
            WHERE e.experiment_id = :experimentId
            GROUP BY v.id, v.name, v.variant_type
            ORDER BY v.variant_type ASC
            """, nativeQuery = true)
    List<Object[]> getVariantStatistics(@Param("experimentId") Long experimentId);

    /**
     * 获取日趋势数据
     */
    @Query(value = """
            SELECT
                DATE(e.create_time) as date,
                SUM(CASE WHEN v.variant_type = 'A' AND e.event_type = 'view' THEN 1 ELSE 0 END) as variant_a_views,
                SUM(CASE WHEN v.variant_type = 'B' AND e.event_type = 'view' THEN 1 ELSE 0 END) as variant_b_views,
                SUM(CASE WHEN v.variant_type = 'A' AND e.event_type = 'conversion' THEN 1 ELSE 0 END) as variant_a_conversions,
                SUM(CASE WHEN v.variant_type = 'B' AND e.event_type = 'conversion' THEN 1 ELSE 0 END) as variant_b_conversions
            FROM ab_event e
            JOIN ab_variant v ON e.variant_id = v.id
            WHERE e.experiment_id = :experimentId
            GROUP BY DATE(e.create_time)
            ORDER BY date ASC
            """, nativeQuery = true)
    List<Object[]> getDailyTrend(@Param("experimentId") Long experimentId);

    /**
     * 获取时间范围内的日趋势数据
     */
    @Query(value = """
            SELECT
                DATE(e.create_time) as date,
                SUM(CASE WHEN v.variant_type = 'A' AND e.event_type = 'view' THEN 1 ELSE 0 END) as variant_a_views,
                SUM(CASE WHEN v.variant_type = 'B' AND e.event_type = 'view' THEN 1 ELSE 0 END) as variant_b_views,
                SUM(CASE WHEN v.variant_type = 'A' AND e.event_type = 'conversion' THEN 1 ELSE 0 END) as variant_a_conversions,
                SUM(CASE WHEN v.variant_type = 'B' AND e.event_type = 'conversion' THEN 1 ELSE 0 END) as variant_b_conversions
            FROM ab_event e
            JOIN ab_variant v ON e.variant_id = v.id
            WHERE e.experiment_id = :experimentId
              AND e.create_time >= :startTime
              AND e.create_time < :endTime
            GROUP BY DATE(e.create_time)
            ORDER BY date ASC
            """, nativeQuery = true)
    List<Object[]> getDailyTrendByDateRange(@Param("experimentId") Long experimentId,
                                             @Param("startTime") Timestamp startTime,
                                             @Param("endTime") Timestamp endTime);
}
