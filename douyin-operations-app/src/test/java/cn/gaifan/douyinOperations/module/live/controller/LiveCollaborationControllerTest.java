package cn.gaifan.douyinOperations.module.live.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LiveCollaborationController WebSocket 集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveCollaborationController WebSocket 集成测试")
class LiveCollaborationControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private final String WS_URL = "ws://localhost:8080/ws";

    @BeforeEach
    void setUp() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    @DisplayName("WebSocket 协作编辑 - 发送编辑事件")
    void handleEdit_shouldBroadcastEvent() throws Exception {
        CompletableFuture<LiveCollaborationController.CollabEvent> resultFuture = new CompletableFuture<>();

        StompSession session = stompClient.connectAsync(WS_URL, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/session/1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LiveCollaborationController.CollabEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                resultFuture.complete((LiveCollaborationController.CollabEvent) payload);
            }
        });

        LiveCollaborationController.CollabEvent event = new LiveCollaborationController.CollabEvent();
        event.setType("edit");
        event.setScriptId(100L);
        event.setUserId(1L);
        event.setUsername("testuser");
        event.setContent("测试内容");

        session.send("/app/session/1/edit", event);

        LiveCollaborationController.CollabEvent result = resultFuture.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("edit");
        assertThat(result.getScriptId()).isEqualTo(100L);
        assertThat(result.getTimestamp()).isGreaterThan(0);

        session.disconnect();
    }

    @Test
    @DisplayName("WebSocket 协作编辑 - 发送光标事件")
    void handleCursor_shouldBroadcastEvent() throws Exception {
        CompletableFuture<LiveCollaborationController.CollabEvent> resultFuture = new CompletableFuture<>();

        StompSession session = stompClient.connectAsync(WS_URL, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/session/1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LiveCollaborationController.CollabEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                resultFuture.complete((LiveCollaborationController.CollabEvent) payload);
            }
        });

        LiveCollaborationController.CollabEvent event = new LiveCollaborationController.CollabEvent();
        event.setUserId(1L);
        event.setUsername("testuser");
        event.setCursorPosition(42);

        session.send("/app/session/1/cursor", event);

        LiveCollaborationController.CollabEvent result = resultFuture.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("cursor");
        assertThat(result.getCursorPosition()).isEqualTo(42);

        session.disconnect();
    }

    @Test
    @DisplayName("WebSocket 协作编辑 - 用户加入")
    void handleJoin_shouldBroadcastEvent() throws Exception {
        CompletableFuture<LiveCollaborationController.CollabEvent> resultFuture = new CompletableFuture<>();

        StompSession session = stompClient.connectAsync(WS_URL, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/session/1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LiveCollaborationController.CollabEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                resultFuture.complete((LiveCollaborationController.CollabEvent) payload);
            }
        });

        LiveCollaborationController.CollabEvent event = new LiveCollaborationController.CollabEvent();
        event.setUserId(1L);
        event.setUsername("testuser");

        session.send("/app/session/1/join", event);

        LiveCollaborationController.CollabEvent result = resultFuture.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("join");
        assertThat(result.getUserId()).isEqualTo(1L);

        session.disconnect();
    }

    @Test
    @DisplayName("WebSocket 协作编辑 - 用户离开")
    void handleLeave_shouldBroadcastEvent() throws Exception {
        CompletableFuture<LiveCollaborationController.CollabEvent> resultFuture = new CompletableFuture<>();

        StompSession session = stompClient.connectAsync(WS_URL, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/session/1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return LiveCollaborationController.CollabEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                resultFuture.complete((LiveCollaborationController.CollabEvent) payload);
            }
        });

        LiveCollaborationController.CollabEvent event = new LiveCollaborationController.CollabEvent();
        event.setUserId(1L);
        event.setUsername("testuser");

        session.send("/app/session/1/leave", event);

        LiveCollaborationController.CollabEvent result = resultFuture.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("leave");
        assertThat(result.getUserId()).isEqualTo(1L);

        session.disconnect();
    }
}
