package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.ExternalApiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ExternalApiCallLogRepository extends JpaRepository<ExternalApiCallLog, Long>,
        JpaSpecificationExecutor<ExternalApiCallLog> {
}
