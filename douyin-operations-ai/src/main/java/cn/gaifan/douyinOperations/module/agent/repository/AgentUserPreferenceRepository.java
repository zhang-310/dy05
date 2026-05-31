package cn.gaifan.douyinOperations.module.agent.repository;

import cn.gaifan.douyinOperations.module.agent.entity.AgentUserPreference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentUserPreferenceRepository extends JpaRepository<AgentUserPreference, Long> {

    Optional<AgentUserPreference> findByUserIdAndPrefKeyAndPrefValueAndDeleted(Long userId, String prefKey, String prefValue, Integer deleted);

    @Query("SELECT p FROM AgentUserPreference p WHERE p.userId = :userId AND p.prefKey = :key AND p.deleted = 0 ORDER BY p.usageCount DESC, p.lastUsedAt DESC")
    List<AgentUserPreference> findTopByUserIdAndKey(@Param("userId") Long userId, @Param("key") String key, Pageable pageable);
}
