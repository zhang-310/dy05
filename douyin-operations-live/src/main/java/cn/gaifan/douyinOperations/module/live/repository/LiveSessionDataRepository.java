package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveSessionData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveSessionDataRepository extends JpaRepository<LiveSessionData, Long> {

    Optional<LiveSessionData> findBySessionId(Long sessionId);

    List<LiveSessionData> findBySessionIdIn(List<Long> sessionIds);
}
