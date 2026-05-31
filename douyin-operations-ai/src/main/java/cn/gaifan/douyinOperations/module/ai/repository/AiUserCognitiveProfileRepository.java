package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiUserCognitiveProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户认知画像 Repository（Phase 3.3）
 */
public interface AiUserCognitiveProfileRepository extends JpaRepository<AiUserCognitiveProfile, Long> {

    Optional<AiUserCognitiveProfile> findByOwnerIdAndDeleted(Long ownerId, Integer deleted);
}
