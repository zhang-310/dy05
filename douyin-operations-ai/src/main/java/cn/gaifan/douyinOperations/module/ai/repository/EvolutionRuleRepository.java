package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.EvolutionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Evolution Rule Repository
 */
public interface EvolutionRuleRepository extends JpaRepository<EvolutionRule, Long> {

    /**
     * Find rule by rule type
     */
    Optional<EvolutionRule> findByRuleTypeAndDeleted(String ruleType, Integer deleted);

    /**
     * Find enabled rule by type
     */
    Optional<EvolutionRule> findByRuleTypeAndIsEnabledAndDeleted(String ruleType, Integer isEnabled, Integer deleted);
}
