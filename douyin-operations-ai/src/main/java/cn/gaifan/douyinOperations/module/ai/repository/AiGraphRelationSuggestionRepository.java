package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiGraphRelationSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiGraphRelationSuggestionRepository extends JpaRepository<AiGraphRelationSuggestion, Long> {

    List<AiGraphRelationSuggestion> findTop50ByOwnerIdAndStatusAndDeletedOrderByCreateTimeDesc(
            Long ownerId, String status, int deleted);

    boolean existsByOwnerIdAndSourceEntityKeyAndTargetEntityKeyAndRelationTypeAndDeleted(
            Long ownerId, String sourceEntityKey, String targetEntityKey, String relationType, Integer deleted);

    Optional<AiGraphRelationSuggestion> findByIdAndOwnerIdAndDeleted(Long id, Long ownerId, Integer deleted);
}
