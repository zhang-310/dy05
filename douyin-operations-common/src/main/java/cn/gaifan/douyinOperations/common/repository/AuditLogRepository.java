package cn.gaifan.douyinOperations.common.repository;

import cn.gaifan.douyinOperations.common.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志仓库接口
 *
 * @author gaifan
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * 查询指定用户的审计日志
     */
    List<AuditLog> findByUsernameOrderByCreateTimeDesc(String username);

    /**
     * 查询指定用户在时间范围内的审计日志
     */
    @Query("SELECT a FROM AuditLog a WHERE a.username = :username AND a.createTime >= :startDate AND a.createTime <= :endDate ORDER BY a.createTime DESC")
    List<AuditLog> findByUsernameAndDateRange(@Param("username") String username,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 查询指定操作类型的审计日志
     */
    List<AuditLog> findByActionOrderByCreateTimeDesc(String action);

    /**
     * 查询失败的操作
     */
    @Query("SELECT a FROM AuditLog a WHERE a.status = 0 ORDER BY a.createTime DESC")
    List<AuditLog> findFailedOperations();

    /**
     * 统计用户的操作数
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.username = :username AND a.createTime >= :startDate")
    long countByUsernameAndDateRange(@Param("username") String username, @Param("startDate") LocalDateTime startDate);

    /**
     * 分页查询审计日志
     */
    Page<AuditLog> findByUsernameContainingOrActionContaining(String username, String action, Pageable pageable);
}

