package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDailyBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 短视频每日批量生成 Repository
 */
public interface SvDailyBatchRepository extends JpaRepository<SvDailyBatch, Long> {

    List<SvDailyBatch> findByOwnerIdAndDeletedOrderByCreateTimeDesc(Long ownerId, int deleted);

    Page<SvDailyBatch> findByOwnerIdAndDeleted(Long ownerId, int deleted, Pageable pageable);
}
