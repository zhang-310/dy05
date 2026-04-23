package cn.gaifan.douyinOperations.module.log.config;

import cn.gaifan.douyinOperations.module.log.service.SystemLogService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 应用启动/关闭时记录系统日志
 */
@Component
public class SystemLogStartupListener {

    @Resource
    private SystemLogService systemLogService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        systemLogService.save("config", "startup", "应用启动完成", null, 1);
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosed(ContextClosedEvent event) {
        systemLogService.save("config", "shutdown", "应用关闭", null, 1);
    }
}
