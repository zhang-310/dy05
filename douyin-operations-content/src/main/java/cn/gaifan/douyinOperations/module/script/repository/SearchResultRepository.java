package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.SearchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 搜索结果 Repository
 */
@Repository
public interface SearchResultRepository extends JpaRepository<SearchResult, Long>,
        JpaSpecificationExecutor<SearchResult> {

    Optional<SearchResult> findByIdAndDeleted(Long id, Integer deleted);

    @Query(value = "SELECT * FROM sc_search_result WHERE owner_id = :ownerId AND deleted = 0 " +
            "ORDER BY created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<SearchResult> findLatestByOwner(@Param("ownerId") Long ownerId,
                                         @Param("limit") int limit);

    @Query(value = "SELECT * FROM sc_search_result WHERE owner_id = :ownerId AND created_at >= :startTime " +
            "AND deleted = 0 ORDER BY created_at DESC",
            nativeQuery = true)
    List<SearchResult> findByOwnerAndTimeRange(@Param("ownerId") Long ownerId,
                                               @Param("startTime") Timestamp startTime);

    @Query(value = "SELECT COUNT(*) FROM sc_search_result WHERE owner_id = :ownerId AND created_at >= :startTime " +
            "AND clicked_result_id IS NOT NULL AND deleted = 0",
            nativeQuery = true)
    long countClickedSearches(@Param("ownerId") Long ownerId,
                             @Param("startTime") Timestamp startTime);

    @Query(value = "SELECT COUNT(*) FROM sc_search_result WHERE owner_id = :ownerId AND created_at >= :startTime " +
            "AND deleted = 0",
            nativeQuery = true)
    long countTotalSearches(@Param("ownerId") Long ownerId,
                           @Param("startTime") Timestamp startTime);
}
