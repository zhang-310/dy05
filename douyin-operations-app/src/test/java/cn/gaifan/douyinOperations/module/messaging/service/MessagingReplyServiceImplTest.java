package cn.gaifan.douyinOperations.module.messaging.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.service.impl.MessagingReplyServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessagingReplyService 单元测试")
class MessagingReplyServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MessagingReplyServiceImpl messagingReplyService;

    private MsgPlatformConfig wecomConfig;
    private MsgPlatformConfig feishuConfig;

    @BeforeEach
    void setUp() {
        // 企微配置
        wecomConfig = new MsgPlatformConfig();
        wecomConfig.setId(1L);
        wecomConfig.setPlatform("wecom");
        wecomConfig.setCorpId("test-corp-id");
        wecomConfig.setSecret("test-secret");
        wecomConfig.setAppId("1000002");

        // 飞书配置
        feishuConfig = new MsgPlatformConfig();
        feishuConfig.setId(2L);
        feishuConfig.setPlatform("feishu");
        feishuConfig.setAppId("test-app-id");
        feishuConfig.setSecret("test-app-secret");

        // 注入 ObjectMapper
        ReflectionTestUtils.setField(messagingReplyService, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("发送文本消息 - config 为 null 应忽略")
    void sendText_nullConfig_shouldIgnore() {
        // When
        messagingReplyService.sendText(null, "user123", "测试消息");

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("发送文本消息 - receiveId 为 null 应忽略")
    void sendText_nullReceiveId_shouldIgnore() {
        // When
        messagingReplyService.sendText(wecomConfig, null, "测试消息");

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("发送文本消息 - content 为 null 应忽略")
    void sendText_nullContent_shouldIgnore() {
        // When
        messagingReplyService.sendText(wecomConfig, "user123", null);

        // Then
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("发送文本消息 - 不支持的平台应抛出异常")
    void sendText_unsupportedPlatform_shouldThrowException() {
        // Given
        MsgPlatformConfig invalidConfig = new MsgPlatformConfig();
        invalidConfig.setPlatform("invalid");

        // When & Then
        assertThatThrownBy(() -> messagingReplyService.sendText(invalidConfig, "user123", "测试消息"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的平台");
    }

    @Test
    @DisplayName("发送企微消息 - 成功应不抛出异常")
    void sendText_wecom_success_shouldNotThrowException() {
        // Given
        String tokenResponse = "{\"access_token\":\"test-token\",\"expires_in\":7200}";
        String successResponse = "{\"errcode\":0,\"errmsg\":\"ok\"}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));

        // When & Then
        assertThatCode(() -> messagingReplyService.sendText(wecomConfig, "user123", "测试消息"))
                .doesNotThrowAnyException();
        verify(restTemplate).getForEntity(anyString(), eq(String.class));
        verify(restTemplate).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("发送企微消息 - HTTP 错误应抛出异常")
    void sendText_wecom_httpError_shouldThrowException() {
        // Given
        String tokenResponse = "{\"access_token\":\"test-token\",\"expires_in\":7200}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // When & Then
        assertThatThrownBy(() -> messagingReplyService.sendText(wecomConfig, "user123", "测试消息"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("企微消息发送失败");
    }

    @Test
    @DisplayName("发送企微消息 - 业务错误应抛出异常")
    void sendText_wecom_businessError_shouldThrowException() {
        // Given
        String tokenResponse = "{\"access_token\":\"test-token\",\"expires_in\":7200}";
        String errorResponse = "{\"errcode\":40001,\"errmsg\":\"invalid credential\"}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));

        // When & Then
        assertThatThrownBy(() -> messagingReplyService.sendText(wecomConfig, "user123", "测试消息"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("企微消息发送失败");
    }

    @Test
    @DisplayName("发送飞书消息 - 成功应不抛出异常")
    void sendText_feishu_success_shouldNotThrowException() {
        // Given
        String tokenResponse = "{\"code\":0,\"tenant_access_token\":\"test-token\"}";
        String successResponse = "{\"code\":0,\"msg\":\"success\"}";
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));

        // When & Then
        assertThatCode(() -> messagingReplyService.sendText(feishuConfig, "ou_123456", "测试消息"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("发送飞书消息 - HTTP 错误应抛出异常")
    void sendText_feishu_httpError_shouldThrowException() {
        // Given
        String tokenResponse = "{\"code\":0,\"tenant_access_token\":\"test-token\"}";
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // When & Then
        assertThatThrownBy(() -> messagingReplyService.sendText(feishuConfig, "ou_123456", "测试消息"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("飞书消息发送失败");
    }

    @Test
    @DisplayName("发送飞书消息 - 业务错误应抛出异常")
    void sendText_feishu_businessError_shouldThrowException() {
        // Given
        String tokenResponse = "{\"code\":0,\"tenant_access_token\":\"test-token\"}";
        String errorResponse = "{\"code\":99991663,\"msg\":\"app access token invalid\"}";
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));

        // When & Then
        assertThatThrownBy(() -> messagingReplyService.sendText(feishuConfig, "ou_123456", "测试消息"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("飞书消息发送失败");
    }
}
