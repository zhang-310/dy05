package cn.gaifan.douyinOperations.module.messaging.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingPlatformService;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingWebhookHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("MessagingWebhookController 集成测试")
class MessagingWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessagingPlatformService messagingPlatformService;

    @MockBean
    private MessagingWebhookHandler messagingWebhookHandler;

    @Test
    @DisplayName("飞书 URL 验证回调 - 应返回 200")
    void feishuPost_urlVerification_shouldReturn200() throws Exception {
        MsgPlatformConfig config = new MsgPlatformConfig();
        config.setId(1L);
        config.setPlatform("feishu");
        config.setCallbackToken("test-token");

        Map<String, String> verifyResponse = new HashMap<>();
        verifyResponse.put("challenge", "test-challenge");

        when(messagingPlatformService.getConfigEntityByPlatformAndToken(eq("feishu"), eq("test-token")))
                .thenReturn(config);

        when(messagingWebhookHandler.handleFeishuUrlVerify(anyString()))
                .thenReturn(verifyResponse);

        String body = "{\"type\":\"url_verification\",\"challenge\":\"test-challenge\"}";

        mockMvc.perform(post("/api/v1/messaging/webhook/feishu")
                        .param("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("飞书事件回调 - 应返回 200")
    void feishuPost_event_shouldReturn200() throws Exception {
        MsgPlatformConfig config = new MsgPlatformConfig();
        config.setId(1L);
        config.setPlatform("feishu");
        config.setCallbackToken("test-token");

        when(messagingPlatformService.getConfigEntityByPlatformAndToken(eq("feishu"), eq("test-token")))
                .thenReturn(config);

        doNothing().when(messagingWebhookHandler).handleFeishuEvent(anyString(), any(MsgPlatformConfig.class));

        String body = "{\"type\":\"event_callback\",\"event\":{\"type\":\"message\"}}";

        mockMvc.perform(post("/api/v1/messaging/webhook/feishu")
                        .param("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("飞书回调（无效 token）- 应返回错误")
    void feishuPost_invalidToken_shouldReturnError() throws Exception {
        when(messagingPlatformService.getConfigEntityByPlatformAndToken(eq("feishu"), eq("invalid-token")))
                .thenReturn(null);

        String body = "{\"type\":\"event_callback\"}";

        mockMvc.perform(post("/api/v1/messaging/webhook/feishu")
                        .param("token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(3903));
    }

    @Test
    @DisplayName("飞书回调（空 body）- 应返回 1001")
    void feishuPost_emptyBody_shouldReturn1001() throws Exception {
        MsgPlatformConfig config = new MsgPlatformConfig();
        config.setId(1L);
        config.setPlatform("feishu");
        config.setCallbackToken("test-token");

        when(messagingPlatformService.getConfigEntityByPlatformAndToken(eq("feishu"), eq("test-token")))
                .thenReturn(config);

        mockMvc.perform(post("/api/v1/messaging/webhook/feishu")
                        .param("token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("企微 URL 验证 - 应返回验证字符串")
    void wecomVerify_shouldReturnEchostr() throws Exception {
        MsgPlatformConfig config = new MsgPlatformConfig();
        config.setId(1L);
        config.setPlatform("wecom");
        config.setCallbackToken("test-token");

        when(messagingPlatformService.getConfigEntityByPlatformAndToken(eq("wecom"), eq("test-token")))
                .thenReturn(config);

        when(messagingWebhookHandler.handleWecomUrlVerify(
                eq("signature"), eq("123456"), eq("nonce"), eq("echostr"), any(MsgPlatformConfig.class)))
                .thenReturn("echostr");

        mockMvc.perform(get("/api/v1/messaging/webhook/wecom")
                        .param("token", "test-token")
                        .param("msg_signature", "signature")
                        .param("timestamp", "123456")
                        .param("nonce", "nonce")
                        .param("echostr", "echostr"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("企微消息回调 - 应返回 success")
    void wecomPost_shouldReturnSuccess() throws Exception {
        MsgPlatformConfig config = new MsgPlatformConfig();
        config.setId(1L);
        config.setPlatform("wecom");
        config.setCallbackToken("test-token");

        when(messagingPlatformService.getConfigEntityByPlatformAndToken(eq("wecom"), eq("test-token")))
                .thenReturn(config);

        doNothing().when(messagingWebhookHandler).handleWecomMessage(
                anyString(), anyString(), anyString(), anyString(), any(MsgPlatformConfig.class));

        String body = "<xml><ToUserName><![CDATA[test]]></ToUserName></xml>";

        mockMvc.perform(post("/api/v1/messaging/webhook/wecom")
                        .param("token", "test-token")
                        .param("msg_signature", "signature")
                        .param("timestamp", "123456")
                        .param("nonce", "nonce")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(body))
                .andExpect(status().isOk());
    }
}
