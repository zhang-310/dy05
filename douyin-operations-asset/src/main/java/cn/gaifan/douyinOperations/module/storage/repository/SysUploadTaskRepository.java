package cn.gaifan.douyinOperations.module.storage.repository;

import cn.gaifan.douyinOperations.module.storage.entity.SysUploadTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface SysUploadTaskRepository extends JpaRepository<SysUploadTask, Long>, JpaSpecificationExecutor<SysUploadTask> {

    Optional<SysUploadTask> findByUploadId(String uploadId);

    Optional<SysUploadTask> findByFileMd5AndOwnerIdAndFileSizeAndDeleted(String fileMd5, Long ownerId, Long fileSize, Integer deleted);

    Page<SysUploadTask> findByOwnerIdAndDeletedOrderByCreatedAtDesc(Long ownerId, Integer deleted, Pageable pageable);

    List<SysUploadTask> findByOwnerIdAndStatusAndExpireAtBeforeAndDeleted(Long ownerId, String status, Timestamp expireAt, Integer deleted);

    @Modifying
    @Query("UPDATE SysUploadTask t SET t.status = :status, t.updatedAt = :updatedAt WHERE t.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedAt") Timestamp updatedAt);

    @Modifying
    @Query("UPDATE SysUploadTask t SET t.uploadedChunks = :uploadedChunks, t.uploadedBytes = :uploadedBytes, t.updatedAt = :updatedAt WHERE t.id = :id")
    void updateProgress(@Param("id") Long id, @Param("uploadedChunks") Integer uploadedChunks,
                       @Param("uploadedBytes") Long uploadedBytes, @Param("updatedAt") Timestamp updatedAt);

    @Modifying
    @Query("UPDATE SysUploadTask t SET t.deleted = 1, t.updatedAt = :updatedAt WHERE t.expireAt < :expireTime AND t.deleted = 0")
    int deleteExpiredTasks(@Param("expireTime") Timestamp expireTime, @Param("updatedAt") Timestamp updatedAt);
}
