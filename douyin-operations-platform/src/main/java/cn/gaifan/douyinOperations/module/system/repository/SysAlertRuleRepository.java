package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.SysAlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysAlertRuleRepository extends JpaRepository<SysAlertRule, Long> {
    Page<SysAlertRule> findByDeleted(Integer deleted, Pageable pageable);
    List<SysAlertRule> findByEnabledAndDeleted(Boolean enabled, Integer deleted);
}
