package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgentReviewRepository extends JpaRepository<AgentReview, Long>, JpaSpecificationExecutor<AgentReview> {

    /** 查找智能体的所有可见评论 */
    Page<AgentReview> findByAgentIdAndStatusAndDeletedOrderByCreateTimeDesc(Long agentId, Integer status, Integer deleted, Pageable pageable);

    /** 用户是否已评论过某智能体 */
    boolean existsByAgentIdAndUserIdAndDeleted(Long agentId, Long userId, Integer deleted);

    /** 用户对某智能体的评论 */
    AgentReview findByAgentIdAndUserIdAndDeleted(Long agentId, Long userId, Integer deleted);

    /** 智能体评论总数 */
    long countByAgentIdAndDeleted(Long agentId, Integer deleted);

    /** 智能体平均评分（四舍五入） */
    @Query("SELECT AVG(r.rating) FROM AgentReview r WHERE r.agentId = :agentId AND r.status = 1 AND r.deleted = 0")
    Double averageRatingByAgentId(@Param("agentId") Long agentId);

    /** 评分统计：各星级数量 */
    @Query("SELECT r.rating, COUNT(r) FROM AgentReview r WHERE r.agentId = :agentId AND r.status = 1 AND r.deleted = 0 GROUP BY r.rating")
    Object[] ratingDistributionByAgentId(@Param("agentId") Long agentId);
}
