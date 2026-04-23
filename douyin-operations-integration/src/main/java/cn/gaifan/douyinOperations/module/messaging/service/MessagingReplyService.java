package cn.gaifan.douyinOperations.module.messaging.service;

import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;

/**
 * 企微/飞书消息回复服务
 */
public interface MessagingReplyService {

    /**
     * 发送文本消息到指定接收者
     * @param config 平台配置（含 corp_id, secret, app_id 等）
     * @param receiveId 接收者 ID（企微为 userid，飞书为 open_id）
     * @param content 文本内容
     */
    void sendText(MsgPlatformConfig config, String receiveId, String content);
}
