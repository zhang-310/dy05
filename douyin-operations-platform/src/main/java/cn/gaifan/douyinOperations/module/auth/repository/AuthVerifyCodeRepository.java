package cn.gaifan.douyinOperations.module.auth.repository;

import cn.gaifan.douyinOperations.module.auth.entity.AuthVerifyCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 验证码记录表 Repository
 */
public interface AuthVerifyCodeRepository extends JpaRepository<AuthVerifyCode, Long> {

    /**
     * 查询某 target+type 下未使用且未过期的验证码，按创建时间倒序取最新一条
     */
    Optional<AuthVerifyCode> findFirstByTargetAndTypeAndUsedAndExpireAtAfterOrderByCreateTimeDesc(
            String target, String type, Integer used, Timestamp expireAt);
}
