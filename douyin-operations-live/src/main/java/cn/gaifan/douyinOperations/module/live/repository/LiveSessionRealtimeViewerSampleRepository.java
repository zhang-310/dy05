package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveSessionRealtimeViewerSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveSessionRealtimeViewerSampleRepository extends JpaRepository<LiveSessionRealtimeViewerSample, Long> {

    List<LiveSessionRealtimeViewerSample> findByLiveSessionIdOrderBySampledAtAsc(Long liveSessionId);
}
