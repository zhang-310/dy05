package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.SearchSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 搜索建议 Repository
 */
@Repository
public interface SearchSuggestionRepository extends JpaRepository<SearchSuggestion, Long>,
        JpaSpecificationExecutor<SearchSuggestion> {

    Optional<SearchSuggestion> findByIdAndDeleted(Long id, Integer deleted);

    @Query(value = "SELECT * FROM sc_search_suggestion WHERE suggestion_text LIKE CONCAT(:prefix, '%') AND deleted = 0 AND (owner_id = :ownerId OR owner_id IS NULL) "
            + "ORDER BY trending_score DESC, search_count DESC LIMIT :limit",
            nativeQuery = true)
    List<SearchSuggestion> findSuggestionsByPrefix(@Param("prefix") String prefix,
                                                     @Param("ownerId") Long ownerId,
                                                     @Param("limit") int limit);

    @Query(value = "SELECT * FROM sc_search_suggestion WHERE suggestion_type = :type AND deleted = 0 " +
            "AND (owner_id = :ownerId OR owner_id IS NULL) " +
            "ORDER BY trending_score DESC, search_count DESC LIMIT :limit",
            nativeQuery = true)
    List<SearchSuggestion> findByTypeOrderByTrendingScore(@Param("type") String type,
                                                          @Param("ownerId") Long ownerId,
                                                          @Param("limit") int limit);

    @Query(value = "SELECT * FROM sc_search_suggestion WHERE suggestion_text = :text AND owner_id = :ownerId AND deleted = 0",
            nativeQuery = true)
    Optional<SearchSuggestion> findByTextAndOwner(@Param("text") String text,
                                                  @Param("ownerId") Long ownerId);

    @Query(value = "SELECT * FROM sc_search_suggestion WHERE suggestion_text = :text AND owner_id IS NULL AND deleted = 0",
            nativeQuery = true)
    Optional<SearchSuggestion> findGlobalSuggestion(@Param("text") String text);

    @Query(value = "SELECT * FROM sc_search_suggestion WHERE owner_id = :ownerId AND deleted = 0 " +
            "ORDER BY trending_score DESC LIMIT :limit",
            nativeQuery = true)
    List<SearchSuggestion> findTopSuggestionsByOwner(@Param("ownerId") Long ownerId,
                                                     @Param("limit") int limit);
}
