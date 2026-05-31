package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiEvolveTopicRepository extends JpaRepository<AiEvolveTopic, Long> {

    Optional<AiEvolveTopic> findByTopicAndKbIdAndDeleted(String topic, Long kbId, Integer deleted);

    List<AiEvolveTopic> findByStatusAndDeletedOrderByPriorityAscCreateTimeDesc(Integer status, Integer deleted);

    /** 进化采样：仅取归属知识库(kbId)或全局(kbId=null)的主题，排除归属抖音(accountId!=null) */
    @Query("SELECT t FROM AiEvolveTopic t WHERE t.status = 1 AND t.deleted = 0 " +
            "AND t.accountId IS NULL AND (t.kbId IS NULL OR t.kbId = :kbId) " +
            "ORDER BY t.kbId ASC NULLS LAST, t.priority ASC, t.lastUsedTime ASC NULLS FIRST")
    List<AiEvolveTopic> findTopicsForSampling(@Param("kbId") Long kbId, Pageable pageable);

    /** 按 category 前缀过滤的进化采样（kb 类型细分用） */
    @Query("SELECT t FROM AiEvolveTopic t WHERE t.status = 1 AND t.deleted = 0 " +
            "AND t.accountId IS NULL AND (t.kbId IS NULL OR t.kbId = :kbId) " +
            "AND t.category LIKE :categoryPrefix " +
            "ORDER BY t.kbId ASC NULLS LAST, t.priority ASC, t.lastUsedTime ASC NULLS FIRST")
    List<AiEvolveTopic> findTopicsForSamplingByCategoryPrefix(@Param("kbId") Long kbId, @Param("categoryPrefix") String categoryPrefix, Pageable pageable);

    @Modifying
    @Query("UPDATE AiEvolveTopic t SET t.usedCount = t.usedCount + 1, t.lastUsedTime = CURRENT_TIMESTAMP " +
            "WHERE t.id IN :ids")
    void incrementUsedCount(@Param("ids") List<Long> ids);

    long countByStatusAndDeleted(Integer status, Integer deleted);

    List<AiEvolveTopic> findByKbIdAndStatusAndDeletedOrderByPriorityAsc(Long kbId, Integer status, Integer deleted);

    List<AiEvolveTopic> findByKbIdIsNullAndStatusAndDeletedOrderByPriorityAsc(Integer status, Integer deleted);

    List<AiEvolveTopic> findByAccountIdAndStatusAndDeletedOrderByPriorityAsc(Long accountId, Integer status, Integer deleted);

    /** 仅全局主题：kbId 与 accountId 均为 null */
    @Query("SELECT t FROM AiEvolveTopic t WHERE t.status = 1 AND t.deleted = 0 AND t.kbId IS NULL AND t.accountId IS NULL ORDER BY t.priority ASC, t.createTime DESC")
    List<AiEvolveTopic> findGlobalTopicsOrderByPriorityAscCreateTimeDesc();

    /** 按 category 统计数量 */
    @Query("SELECT t.category, COUNT(t) FROM AiEvolveTopic t WHERE t.status = 1 AND t.deleted = 0 GROUP BY t.category")
    List<Object[]> countByCategory();

    /** P1 主题池轮换：取尾部 12 个（priority 高、createTime 早）用于替换，仅知识库/全局主题 */
    @Query("SELECT t FROM AiEvolveTopic t WHERE t.status = 1 AND t.deleted = 0 AND t.accountId IS NULL AND (t.kbId = :kbId OR t.kbId IS NULL) " +
            "ORDER BY t.priority DESC, t.createTime ASC")
    List<AiEvolveTopic> findTailTopicsForEviction(@Param("kbId") Long kbId, Pageable pageable);

    /** P1 主题池轮换（按 category 前缀过滤）：针对特定 kb 类型 */
    @Query("SELECT t FROM AiEvolveTopic t WHERE t.status = 1 AND t.deleted = 0 AND t.accountId IS NULL AND (t.kbId = :kbId OR t.kbId IS NULL) AND t.category LIKE :categoryPrefix " +
            "ORDER BY t.priority DESC, t.createTime ASC")
    List<AiEvolveTopic> findTailTopicsForEvictionByCategoryPrefix(@Param("kbId") Long kbId, @Param("categoryPrefix") String categoryPrefix, Pageable pageable);
}
