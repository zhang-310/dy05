package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SvAccountCollectTaskRepository extends JpaRepository<SvAccountCollectTask, Long>,
        JpaSpecificationExecutor<SvAccountCollectTask> {

    Optional<SvAccountCollectTask> findByIdAndDeleted(Long id, Integer deleted);

    List<SvAccountCollectTask> findByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, Integer deleted);

    @Modifying
    @Transactional
    @Query("UPDATE SvAccountCollectTask t SET t.status = :status, t.errorMessage = :errorMessage WHERE t.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("errorMessage") String errorMessage);

    @Modifying
    @Transactional
    @Query("UPDATE SvAccountCollectTask t SET t.analyzedVideos = t.analyzedVideos + 1 WHERE t.id = :id")
    int incrementAnalyzedVideos(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE SvAccountCollectTask t SET t.indexedVideos = t.indexedVideos + 1 WHERE t.id = :id")
    int incrementIndexedVideos(@Param("id") Long id);
}
