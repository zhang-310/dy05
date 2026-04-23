package cn.gaifan.douyinOperations.module.log.config;

import cn.gaifan.douyinOperations.common.event.LoginSuccessEvent;
import cn.gaifan.douyinOperations.module.log.service.OperationLogService;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 监听登录成功事件，记录操作日志（登录接口走白名单无 userId，故用事件单独记录）
 */
@Component
public class LoginSuccessLogListener {

    @Resource
    private OperationLogService operationLogService;

    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        operationLogService.save(
                event.getUserId(),
                event.getUsername(),
                "auth",
                "login",
                event.getRequestUri(),
                event.getRequestMethod(),
                event.getIp(),
                event.getUserAgent(),
                null,
                1,
                null,
                MDC.get("traceId"),
                null,
                null
        );
    }
}
