package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiLiveReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiLiveReviewRepository extends JpaRepository<AiLiveReview, Long>, JpaSpecificationExecutor<AiLiveReview> {

    Optional<AiLiveReview> findByIdAndDeleted(Long id, Integer deleted);

    Optional<AiLiveReview> findBySessionIdAndDeleted(Long sessionId, Integer deleted);

    @Modifying
    @Query("UPDATE AiLiveReview r SET r.status = :status, r.reportContent = :report, r.topScripts = :scripts, r.weakPoints = :weakPoints, r.tokensUsed = :tokens, r.modelUsed = :model WHERE r.id = :id")
    void completeReview(@Param("id") Long id, @Param("status") Integer status,
                        @Param("report") String report, @Param("scripts") String scripts,
                        @Param("weakPoints") String weakPoints,
                        @Param("tokens") Long tokens, @Param("model") String model);
}
