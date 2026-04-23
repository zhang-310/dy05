package cn.gaifan.douyinOperations.module.config.repository;

import cn.gaifan.douyinOperations.module.config.entity.ConfigVersionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 配置版本历史 Repository。
 */
public interface ConfigVersionHistoryRepository extends JpaRepository<ConfigVersionHistory, Long> {

    List<ConfigVersionHistory> findByConfigIdOrderByCreateTimeDesc(Long configId, org.springframework.data.domain.Pageable pageable);
}
