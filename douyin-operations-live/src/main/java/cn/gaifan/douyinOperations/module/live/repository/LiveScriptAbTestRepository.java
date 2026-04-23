package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptAbTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 话术版本 A/B 测试 Repository（Phase 2.5）
 */
public interface LiveScriptAbTestRepository extends JpaRepository<LiveScriptAbTest, Long> {

    Page<LiveScriptAbTest> findByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, Integer deleted, Pageable pageable);

    List<LiveScriptAbTest> findByScriptIdAndStatusAndDeleted(Long scriptId, String status, Integer deleted);
}
