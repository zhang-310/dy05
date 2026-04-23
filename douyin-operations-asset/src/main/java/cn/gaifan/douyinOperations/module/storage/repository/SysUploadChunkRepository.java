package cn.gaifan.douyinOperations.module.storage.repository;

import cn.gaifan.douyinOperations.module.storage.entity.SysUploadChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface SysUploadChunkRepository extends JpaRepository<SysUploadChunk, Long> {

    List<SysUploadChunk> findByTaskIdOrderByChunkIndex(Long taskId);

    List<SysUploadChunk> findByTaskIdAndStatus(Long taskId, String status);

    Optional<SysUploadChunk> findByTaskIdAndChunkIndex(Long taskId, Integer chunkIndex);

    @Modifying
    @Query("UPDATE SysUploadChunk c SET c.status = :status, c.bosEtag = :bosEtag, c.bosPartNumber = :bosPartNumber, c.uploadedAt = :uploadedAt WHERE c.id = :id")
    void updateChunk(@Param("id") Long id, @Param("status") String status, @Param("bosEtag") String bosEtag,
                    @Param("bosPartNumber") Integer bosPartNumber, @Param("uploadedAt") Timestamp uploadedAt);

    @Modifying
    @Query("UPDATE SysUploadChunk c SET c.retryCount = c.retryCount + 1 WHERE c.id = :id")
    void incrementRetryCount(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM SysUploadChunk c WHERE c.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
