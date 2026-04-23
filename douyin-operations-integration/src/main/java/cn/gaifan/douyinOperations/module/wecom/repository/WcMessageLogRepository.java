package cn.gaifan.douyinOperations.module.wecom.repository;

import cn.gaifan.douyinOperations.module.wecom.entity.WcMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface WcMessageLogRepository extends JpaRepository<WcMessageLog, Long>, JpaSpecificationExecutor<WcMessageLog> {

    Optional<WcMessageLog> findById(Long id);
}
