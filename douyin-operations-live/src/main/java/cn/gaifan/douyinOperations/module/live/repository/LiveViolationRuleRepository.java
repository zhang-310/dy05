package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveViolationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface LiveViolationRuleRepository extends JpaRepository<LiveViolationRule, Long>, JpaSpecificationExecutor<LiveViolationRule> {

    List<LiveViolationRule> findByPlatformIdAndActiveAndDeleted(Long platformId, Integer active, Integer deleted);
}
