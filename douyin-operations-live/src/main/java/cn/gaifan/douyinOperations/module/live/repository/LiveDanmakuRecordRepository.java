package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveDanmakuRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface LiveDanmakuRecordRepository extends JpaRepository<LiveDanmakuRecord, Long>, JpaSpecificationExecutor<LiveDanmakuRecord> {
    Optional<LiveDanmakuRecord> findByDouyinCommentIdAndDeleted(String douyinCommentId, int deleted);
    List<LiveDanmakuRecord> findBySessionIdAndDeletedOrderByDanmakuTimeDesc(Long sessionId, int deleted);
    List<LiveDanmakuRecord> findBySessionIdAndDanmakuTimeAfterAndDeleted(Long sessionId, Timestamp after, int deleted);
    long countBySessionIdAndDeleted(Long sessionId, int deleted);
}
