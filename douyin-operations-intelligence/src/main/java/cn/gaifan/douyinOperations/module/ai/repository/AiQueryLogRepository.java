package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.AiQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiQueryLogRepository extends JpaRepository<AiQueryLog, Long> {

    List<AiQueryLog> findTop10ByUserIdOrderByCreateTimeDesc(Long userId);
}
