package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvSubtitleSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SvSubtitleSegmentRepository extends JpaRepository<SvSubtitleSegment, Long> {

    List<SvSubtitleSegment> findByOwnerIdAndVideoIdAndDeletedOrderByStartTimeAsc(
            Long ownerId, Long videoId, Integer deleted);
}
