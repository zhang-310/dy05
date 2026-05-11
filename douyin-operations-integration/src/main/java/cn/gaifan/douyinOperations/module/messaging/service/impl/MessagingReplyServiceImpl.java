package cn.gaifan.douyinOperations.module.messaging.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingReplyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessagingReplyServiceImpl implements MessagingReplyService {

    private static final Logger log = LoggerFactory.getLogger(MessagingReplyServiceImpl.class);

    @Resource
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final long TOKEN_EXPIRE_SEC = 7000;
    private final Map<String, TokenHolder> tokenCache = new ConcurrentHashMap<>();

    @Override
    public void sendText(MsgPlatformConfig config, String receiveId, String content) {
        if (config == null || receiveId == null || content == null) return;
        String platform = config.getPlatform();
        if ("wecom".equalsIgnoreCase(platform)) {
            sendWecom(config, receiveId, content);
        } else if ("feishu".equalsIgnoreCase(platform)) {
            sendFeishu(config, receiveId, content);
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "不支持的平台: " + platform);
        }
    }

    private void sendWecom(MsgPlatformConfig config, String userId, String content) {
        String token = getWecomAccessToken(config);
        String url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=" + token;
        Map<String, Object> body = new HashMap<>();
        body.put("touser", userId);
        body.put("msgtype", "text");
        body.put("agentid", config.getAppId() != null ? Long.parseLong(config.getAppId()) : 1000002);
        body.put("text", Map.of("content", content));
        ResponseEntity<String> resp = restTemplate.postForEntity(url, body, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException(ErrorCode.WECOM_SEND_FAIL, "企微消息发送失败");
        }
        try {
            JsonNode node = objectMapper.readTree(resp.getBody());
            if (node != null && node.has("errcode") && node.get("errcode").asInt() != 0) {
                String err = node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";
                throw new BusinessException(ErrorCode.WECOM_SEND_FAIL, "企微消息发送失败: " + err);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析企微响应失败: {}", e.getMessage());
        }
    }

    private void sendFeishu(MsgPlatformConfig config, String openId, String content) {
        String token = getFeishuAccessToken(config);
        String url = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=open_id";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        Map<String, Object> body = new HashMap<>();
        body.put("receive_id", openId);
        body.put("msg_type", "text");
        body.put("content", objectMapper.createObjectNode().put("text", content));
        HttpEntity<?> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new BusinessException(ErrorCode.WECOM_SEND_FAIL, "飞书消息发送失败");
        }
        try {
            JsonNode node = objectMapper.readTree(resp.getBody());
            if (node != null && node.has("code") && node.get("code").asInt() != 0) {
                String err = node.has("msg") ? node.get("msg").asText() : "未知错误";
                throw new BusinessException(ErrorCode.WECOM_SEND_FAIL, "飞书消息发送失败: " + err);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析飞书响应失败: {}", e.getMessage());
        }
    }

    private String getWecomAccessToken(MsgPlatformConfig config) {
        String key = "wecom:" + config.getCorpId() + ":" + config.getSecret();
        TokenHolder h = tokenCache.get(key);
        if (h != null && !h.isExpired()) return h.token;

        // P0-003: 使用 synchronized 防止并发重复请求 access_token
        synchronized (this) {
            // Double-check: 可能其他线程已获取
            h = tokenCache.get(key);
            if (h != null && !h.isExpired()) return h.token;

            String url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=" + config.getCorpId() + "&corpsecret=" + config.getSecret();
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            try {
                JsonNode node = objectMapper.readTree(resp.getBody());
                if (node == null || !node.has("access_token")) {
                    throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "企微获取 access_token 失败");
                }
                String token = node.get("access_token").asText();
                tokenCache.put(key, new TokenHolder(token));
                return token;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "企微鉴权失败");
            }
        }
    }

    private String getFeishuAccessToken(MsgPlatformConfig config) {
        String key = "feishu:" + config.getAppId() + ":" + config.getSecret();
        TokenHolder h = tokenCache.get(key);
        if (h != null && !h.isExpired()) return h.token;

        // P0-003: 使用 synchronized 防止并发重复请求 access_token
        synchronized (this) {
            // Double-check: 可能其他线程已获取
            h = tokenCache.get(key);
            if (h != null && !h.isExpired()) return h.token;

            String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
            Map<String, String> body = Map.of("app_id", config.getAppId() != null ? config.getAppId() : "",
                    "app_secret", config.getSecret() != null ? config.getSecret() : "");
            ResponseEntity<String> resp = restTemplate.postForEntity(url, body, String.class);
            try {
                JsonNode node = objectMapper.readTree(resp.getBody());
                if (node == null || !node.has("tenant_access_token")) {
                    throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "飞书获取 tenant_access_token 失败");
                }
                String token = node.get("tenant_access_token").asText();
                tokenCache.put(key, new TokenHolder(token));
                return token;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "飞书鉴权失败");
            }
        }
    }

    private static class TokenHolder {
        final String token;
        final long expireAt;

        TokenHolder(String token) {
            this.token = token;
            this.expireAt = System.currentTimeMillis() + TOKEN_EXPIRE_SEC * 1000;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
