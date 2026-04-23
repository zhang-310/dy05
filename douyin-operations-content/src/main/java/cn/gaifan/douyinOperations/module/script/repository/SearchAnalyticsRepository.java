package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.SearchAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

/**
 * 搜索分析 Repository
 */
@Repository
public interface SearchAnalyticsRepository extends JpaRepository<SearchAnalytics, Long>,
        JpaSpecificationExecutor<SearchAnalytics> {

    Optional<SearchAnalytics> findByIdAndDeleted(Long id, Integer deleted);

    @Query(value = "SELECT * FROM sc_search_analytics WHERE owner_id = :ownerId AND analytics_date >= :startDate " +
            "AND deleted = 0 ORDER BY analytics_date DESC",
            nativeQuery = true)
    List<SearchAnalytics> findByOwnerAndDateRange(@Param("ownerId") Long ownerId,
                                                  @Param("startDate") Date startDate);

    @Query(value = "SELECT * FROM sc_search_analytics WHERE owner_id = :ownerId AND analytics_date = :analyticsDate " +
            "AND deleted = 0",
            nativeQuery = true)
    List<SearchAnalytics> findByOwnerAndDate(@Param("ownerId") Long ownerId,
                                            @Param("analyticsDate") Date analyticsDate);

    @Query(value = "SELECT * FROM sc_search_analytics WHERE owner_id = :ownerId AND search_type = :searchType " +
            "AND analytics_date >= :startDate AND deleted = 0 ORDER BY analytics_date DESC",
            nativeQuery = true)
    List<SearchAnalytics> findByOwnerTypeAndDate(@Param("ownerId") Long ownerId,
                                                 @Param("searchType") String searchType,
                                                 @Param("startDate") Date startDate);

    @Query(value = "SELECT * FROM sc_search_analytics WHERE owner_id = :ownerId AND search_query = :query " +
            "AND analytics_date >= :startDate AND deleted = 0 ORDER BY analytics_date DESC",
            nativeQuery = true)
    List<SearchAnalytics> findByOwnerQueryAndDate(@Param("ownerId") Long ownerId,
                                                  @Param("query") String query,
                                                  @Param("startDate") Date startDate);
}
