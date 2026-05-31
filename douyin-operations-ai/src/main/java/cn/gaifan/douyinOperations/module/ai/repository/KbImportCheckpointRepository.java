package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.KbImportCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KbImportCheckpointRepository extends JpaRepository<KbImportCheckpoint, Long> {

    Optional<KbImportCheckpoint> findByKbIdAndSourcePathAndDeleted(Long kbId, String sourcePath, Integer deleted);
}
