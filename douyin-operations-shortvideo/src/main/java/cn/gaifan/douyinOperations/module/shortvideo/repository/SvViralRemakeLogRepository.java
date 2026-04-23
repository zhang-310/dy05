package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralRemakeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SvViralRemakeLogRepository extends JpaRepository<SvViralRemakeLog, Long> {

    List<SvViralRemakeLog> findByViralVideoIdOrderByCreateTimeDesc(Long viralVideoId);

    List<SvViralRemakeLog> findByViralVideoIdOrderByCreateTimeAsc(Long viralVideoId);
}
