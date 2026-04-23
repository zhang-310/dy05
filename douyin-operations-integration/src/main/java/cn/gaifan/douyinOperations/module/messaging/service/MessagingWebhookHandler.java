package cn.gaifan.douyinOperations.module.messaging.service;

import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;

/**
 * 企微/飞书 Webhook 处理：验签、解析、路由到 Agent、回复
 */
public interface MessagingWebhookHandler {

    /**
     * 飞书 URL 验证：解析 challenge 并返回需回传的 Map（序列化为 JSON）
     */
    java.util.Map<String, String> handleFeishuUrlVerify(String requestBody);

    /**
     * 企微 URL 验证：验签并解密 echostr，返回明文
     */
    String handleWecomUrlVerify(String msgSignature, String timestamp, String nonce, String echostr, MsgPlatformConfig config);

    /**
     * 飞书事件回调：解析消息，路由到 Agent，发送回复
     */
    void handleFeishuEvent(String requestBody, MsgPlatformConfig config);

    /**
     * 企微消息回调：验签解密，路由到 Agent，发送回复
     */
    void handleWecomMessage(String requestBody, String msgSignature, String timestamp, String nonce, MsgPlatformConfig config);
}
