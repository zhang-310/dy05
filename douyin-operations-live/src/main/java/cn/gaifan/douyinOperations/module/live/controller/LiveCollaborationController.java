package cn.gaifan.douyinOperations.module.live.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * 实时协作编辑 WebSocket Controller
 * 客户端通过 STOMP 发送编辑事件，广播给同一场次的所有协作者
 */
@Controller
@Tag(name = "实时协作", description = "WebSocket STOMP 协作编辑")
public class LiveCollaborationController {

    @MessageMapping("/session/{sessionId}/edit")
    @SendTo("/topic/session/{sessionId}")
    public CollabEvent handleEdit(@DestinationVariable Long sessionId, CollabEvent event) {
        event.setSessionId(sessionId);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }

    @MessageMapping("/session/{sessionId}/cursor")
    @SendTo("/topic/session/{sessionId}")
    public CollabEvent handleCursor(@DestinationVariable Long sessionId, CollabEvent event) {
        event.setSessionId(sessionId);
        event.setType("cursor");
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }

    @MessageMapping("/session/{sessionId}/join")
    @SendTo("/topic/session/{sessionId}")
    public CollabEvent handleJoin(@DestinationVariable Long sessionId, CollabEvent event) {
        event.setSessionId(sessionId);
        event.setType("join");
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }

    @MessageMapping("/session/{sessionId}/leave")
    @SendTo("/topic/session/{sessionId}")
    public CollabEvent handleLeave(@DestinationVariable Long sessionId, CollabEvent event) {
        event.setSessionId(sessionId);
        event.setType("leave");
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }

    @Data
    public static class CollabEvent {
        private Long sessionId;
        private String type;
        private Long scriptId;
        private Long userId;
        private String username;
        private String content;
        private Integer cursorPosition;
        private long timestamp;
    }
}
