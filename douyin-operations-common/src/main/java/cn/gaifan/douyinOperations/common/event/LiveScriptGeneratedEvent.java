package cn.gaifan.douyinOperations.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 直播话术生成事件（全场/单段生成完成时发布，供效果归因、模板入库等监听）
 */
public class LiveScriptGeneratedEvent extends ApplicationEvent {

    private final Long sessionId;
    private final Long scriptId;
    private final String scriptType;
    private final Long userId;

    public LiveScriptGeneratedEvent(Object source, Long sessionId, Long scriptId, String scriptType, Long userId) {
        super(source);
        this.sessionId = sessionId;
        this.scriptId = scriptId;
        this.scriptType = scriptType;
        this.userId = userId;
    }

    public Long getSessionId() { return sessionId; }
    public Long getScriptId() { return scriptId; }
    public String getScriptType() { return scriptType; }
    public Long getUserId() { return userId; }
}
