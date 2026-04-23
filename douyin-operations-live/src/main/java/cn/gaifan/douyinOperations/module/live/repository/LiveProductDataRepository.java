package cn.gaifan.douyinOperations.module.live.repository;

import cn.gaifan.douyinOperations.module.live.entity.LiveProductData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveProductDataRepository extends JpaRepository<LiveProductData, Long> {

    List<LiveProductData> findBySessionId(Long sessionId);

    Optional<LiveProductData> findBySessionIdAndProductId(Long sessionId, Long productId);
}
