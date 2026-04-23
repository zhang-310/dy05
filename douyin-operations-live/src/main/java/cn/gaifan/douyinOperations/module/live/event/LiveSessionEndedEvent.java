package cn.gaifan.douyinOperations.module.live.event;

import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import org.springframework.context.ApplicationEvent;

/**
 * 直播场次结束时发布的事件，用于触发抖音数据同步等后续处理。
 */
public class LiveSessionEndedEvent extends ApplicationEvent {

    private final LiveSession session;

    public LiveSessionEndedEvent(Object source, LiveSession session) {
        super(source);
        this.session = session;
    }

    public LiveSession getSession() {
        return session;
    }
}
