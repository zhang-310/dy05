package cn.gaifan.douyinOperations.module.live.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 实时建议 SSE 推送服务（P0-11）
 * 注册 session 的 SseEmitter，定时评估指标并推送建议
 */
public interface SuggestionPushService {

    /**
     * 注册场次的 SSE 订阅
     *
     * @param sessionId 直播场次 ID
     * @param userId    当前用户 ID
     * @param emitter   SseEmitter
     */
    void register(Long sessionId, Long userId, SseEmitter emitter);

    /**
     * 取消注册
     *
     * @param sessionId 直播场次 ID
     * @param emitter   SseEmitter
     */
    void unregister(Long sessionId, SseEmitter emitter);
}
