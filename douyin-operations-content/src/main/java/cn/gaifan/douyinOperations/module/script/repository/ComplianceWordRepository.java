package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ComplianceWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 合规词库 Repository
 */
public interface ComplianceWordRepository extends JpaRepository<ComplianceWord, Long> {

    List<ComplianceWord> findByWordTypeAndIsEnabled(String wordType, Integer isEnabled);
}
