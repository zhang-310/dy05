package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.module.auth.entity.AuthLoginLog;
import cn.gaifan.douyinOperations.module.auth.repository.AuthLoginLogRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

/**
 * 登录日志服务
 * 使用独立事务，确保失败登录也能记录
 */
@Service
public class AuthLoginLogService {

    private static final Logger log = LoggerFactory.getLogger(AuthLoginLogService.class);

    @Resource
    private AuthLoginLogRepository authLoginLogRepository;

    /**
     * 记录登录日志（独立事务）
     * 使用 REQUIRES_NEW 确保即使外层事务回滚，日志也能保存
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logLoginAttempt(Long userId, String username, String loginType,
                                String clientIp, Integer success, String failReason) {
        try {
            AuthLoginLog loginLog = new AuthLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setLoginType(loginType);
            loginLog.setDeviceType("web");
            loginLog.setIp(clientIp);
            loginLog.setStatus(success);
            loginLog.setFailReason(failReason);
            loginLog.setLoginTime(new Timestamp(System.currentTimeMillis()));
            authLoginLogRepository.save(loginLog);

            log.debug("登录日志已记录: username={}, success={}, ip={}", username, success, clientIp);
        } catch (Exception e) {
            // 日志记录失败不应影响主流程，仅记录错误
            log.error("记录登录日志失败: username={}, error={}", username, e.getMessage(), e);
        }
    }

    /**
     * 记录成功登录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logSuccessLogin(Long userId, String username, String loginType, String clientIp) {
        logLoginAttempt(userId, username, loginType, clientIp, 1, null);
    }

    /**
     * 记录失败登录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logFailedLogin(String username, String loginType, String clientIp, String reason) {
        logLoginAttempt(null, username, loginType, clientIp, 0, reason);
    }
}
