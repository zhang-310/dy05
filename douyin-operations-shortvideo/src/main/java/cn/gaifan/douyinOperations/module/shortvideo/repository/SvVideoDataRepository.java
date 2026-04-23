package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface SvVideoDataRepository extends JpaRepository<SvVideoData, Long>, JpaSpecificationExecutor<SvVideoData> {

    Optional<SvVideoData> findByVideoIdAndSnapshotDate(Long videoId, Date snapshotDate);

    List<SvVideoData> findByVideoIdOrderBySnapshotDateDesc(Long videoId);

    /** 按视频 ID 列表与日期范围查询，用于 Dashboard 趋势聚合 */
    List<SvVideoData> findByVideoIdInAndSnapshotDateBetween(List<Long> videoIds, Date start, Date end);
}
