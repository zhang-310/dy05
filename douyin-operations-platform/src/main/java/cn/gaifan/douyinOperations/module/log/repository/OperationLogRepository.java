package cn.gaifan.douyinOperations.module.log.repository;

import cn.gaifan.douyinOperations.module.log.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 用户操作日志 Repository
 */
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {
}
