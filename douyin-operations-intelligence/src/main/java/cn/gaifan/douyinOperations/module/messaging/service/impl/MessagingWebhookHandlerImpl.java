package cn.gaifan.douyinOperations.module.messaging.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.agent.service.AgentService;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingReplyService;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingWebhookHandler;
import cn.gaifan.douyinOperations.module.messaging.util.WecomCryptoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MessagingWebhookHandlerImpl implements MessagingWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(MessagingWebhookHandlerImpl.class);

    @Resource
    private AgentService agentService;
    @Resource
    private MessagingReplyService messagingReplyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, String> handleFeishuUrlVerify(String requestBody) {
        try {
            JsonNode node = objectMapper.readTree(requestBody);
            if (node != null && "url_verification".equals(node.path("type").asText(""))) {
                String challenge = node.path("challenge").asText("");
                return Map.of("challenge", challenge);
            }
        } catch (Exception e) {
            log.warn("飞书 URL 验证解析失败: {}", e.getMessage());
        }
        throw new BusinessException(ErrorCode.VALIDATION_FAIL, "飞书 URL 验证失败");
    }

    @Override
    public String handleWecomUrlVerify(String msgSignature, String timestamp, String nonce, String echostr, MsgPlatformConfig config) {
        String token = config.getCallbackToken();
        if (token == null || !WecomCryptoUtil.verifySignature(token, timestamp, nonce, echostr, msgSignature)) {
            throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "企微签名验证失败");
        }
        try {
            String aesKey = config.getCallbackEncodingAesKey();
            if (aesKey == null || aesKey.isBlank()) {
                return echostr;
            }
            return WecomCryptoUtil.decrypt(aesKey, echostr);
        } catch (Exception e) {
            log.warn("企微 echostr 解密失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "企微解密失败");
        }
    }

    @Override
    public void handleFeishuEvent(String requestBody, MsgPlatformConfig config) {
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            String type = root.path("type").asText("");
            if ("url_verification".equals(type)) return;
            JsonNode event = root.path("event");
            if (event == null || event.isMissingNode()) return;
            String eventType = event.path("type").asText("");
            if (!"im.message.receive_v1".equals(eventType)) return;
            JsonNode sender = event.path("sender").path("sender_id");
            String openId = sender.path("open_id").asText(null);
            JsonNode msg = event.path("message");
            String content = "";
            if (msg.has("content")) {
                JsonNode cnt = objectMapper.readTree(msg.get("content").asText());
                if (cnt.has("text")) content = cnt.get("text").asText("");
            }
            if (openId == null || content.isBlank()) return;
            Long agentId = config.getAgentId();
            if (agentId == null) {
                log.warn("飞书配置未绑定 agent_id，跳过回复");
                return;
            }
            Long userId = config.getOwnerId();
            Long convId = agentService.createConversation(agentId, userId, "飞书对话");
            String reply = agentService.chatWithAgent(agentId, userId, content, convId, null, null);
            if (reply != null && !reply.isBlank()) {
                messagingReplyService.sendText(config, openId, reply);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("飞书事件处理失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "飞书事件处理失败");
        }
    }

    @Override
    public void handleWecomMessage(String requestBody, String msgSignature, String timestamp, String nonce, MsgPlatformConfig config) {
        try {
            String encrypt = extractWecomEncrypt(requestBody);
            if (encrypt == null || encrypt.isBlank()) {
                throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "企微消息无 Encrypt 字段");
            }
            String token = config.getCallbackToken();
            if (!WecomCryptoUtil.verifySignature(token, timestamp, nonce, encrypt, msgSignature)) {
                throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "企微签名验证失败");
            }
            String xml = config.getCallbackEncodingAesKey() != null && !config.getCallbackEncodingAesKey().isBlank()
                    ? WecomCryptoUtil.decrypt(config.getCallbackEncodingAesKey(), encrypt)
                    : encrypt;
            String fromUser = extractXmlTag(xml, "FromUserName");
            String content = extractXmlTag(xml, "Content");
            String msgType = extractXmlTag(xml, "MsgType");
            if (!"text".equalsIgnoreCase(msgType)) return;
            if (fromUser == null || content == null || content.isBlank()) return;
            Long agentId = config.getAgentId();
            if (agentId == null) {
                log.warn("企微配置未绑定 agent_id，跳过回复");
                return;
            }
            Long userId = config.getOwnerId();
            Long convId = agentService.createConversation(agentId, userId, "企微对话");
            String reply = agentService.chatWithAgent(agentId, userId, content, convId, null, null);
            if (reply != null && !reply.isBlank()) {
                messagingReplyService.sendText(config, fromUser, reply);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("企微消息处理失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "企微消息处理失败");
        }
    }

    private static String extractXmlTag(String xml, String tag) {
        if (xml == null) return null;
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int s = xml.indexOf(open);
        int e = xml.indexOf(close);
        if (s >= 0 && e > s) return xml.substring(s + open.length(), e).trim();
        String cdataOpen = "<" + tag + "><![CDATA[";
        String cdataClose = "]]></" + tag + ">";
        int cs = xml.indexOf(cdataOpen);
        int ce = xml.indexOf(cdataClose);
        if (cs >= 0 && ce > cs) return xml.substring(cs + cdataOpen.length(), ce).trim();
        return null;
    }

    private static String extractWecomEncrypt(String body) {
        String enc = extractXmlTag(body, "Encrypt");
        if (enc != null) return enc;
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            return root.path("Encrypt").asText(null);
        } catch (Exception ignored) {}
        return null;
    }
}
