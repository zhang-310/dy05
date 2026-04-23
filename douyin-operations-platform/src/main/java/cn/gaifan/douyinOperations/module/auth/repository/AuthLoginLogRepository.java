package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthLoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

/**
 * 登录记录表 Repository
 */
public interface AuthLoginLogRepository extends JpaRepository<AuthLoginLog, Long> {

    Page<AuthLoginLog> findByUserIdOrderByLoginTimeDesc(Long userId, Pageable pageable);

    Page<AuthLoginLog> findAllByOrderByLoginTimeDesc(Pageable pageable);

    /** 最近某时间之后的登录记录，按登录时间倒序（最多 500 条），用于在线用户列表 */
    List<AuthLoginLog> findTop500ByLoginTimeAfterOrderByLoginTimeDesc(Timestamp after);
}
