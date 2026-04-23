package cn.gaifan.douyinOperations.module.storage.repository;

import cn.gaifan.douyinOperations.module.storage.entity.BosFileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * BOS 文件元数据 Repository
 */
public interface BosFileMetadataRepository extends JpaRepository<BosFileMetadata, Long> {

    Optional<BosFileMetadata> findByBosKeyAndDeleted(String bosKey, Integer deleted);

    List<BosFileMetadata> findByUserIdAndTaskIdAndDeletedOrderByCreateTimeAsc(Long userId, Long taskId, Integer deleted);

    }
