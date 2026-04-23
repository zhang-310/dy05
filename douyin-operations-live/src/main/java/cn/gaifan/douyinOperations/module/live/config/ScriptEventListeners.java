package cn.gaifan.douyinOperations.module.live.config;

import cn.gaifan.douyinOperations.common.event.LiveScriptGeneratedEvent;
import cn.gaifan.douyinOperations.module.live.entity.ScriptUsageLog;
import cn.gaifan.douyinOperations.module.live.repository.ScriptUsageLogRepository;
import jakarta.annotation.Resource;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ScriptEventListeners {

    @Resource
    private ScriptUsageLogRepository scriptUsageLogRepository;

    @EventListener
    @Async
    public void onLiveScriptGenerated(LiveScriptGeneratedEvent event) {
        try {
            ScriptUsageLog log = new ScriptUsageLog();
            log.setScriptSource("live");
            log.setScriptId(event.getScriptId());
            log.setSessionId(event.getSessionId());
            log.setScriptType(event.getScriptType());
            log.setUserId(event.getUserId());
            scriptUsageLogRepository.save(log);
        } catch (Exception e) {
            // 归因日志失败不影响主流程
        }
    }
}
