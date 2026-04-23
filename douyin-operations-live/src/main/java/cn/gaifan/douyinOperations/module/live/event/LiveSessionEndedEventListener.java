package cn.gaifan.douyinOperations.module.live.event;

import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.service.CrossSessionLearningService;
import cn.gaifan.douyinOperations.module.live.service.DouyinLiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.service.LiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptAttributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 直播场次结束事件监听器。
 * 触发抖音数据同步（当 liveUrl 和 accessToken 可用时）及 LiveMonitor 汇总。
 * P0-4: 同步触发跨场次学习记忆提取。
 */
@Component
public class LiveSessionEndedEventListener {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionEndedEventListener.class);

    @Resource
    private DouyinLiveDataSyncService douyinLiveDataSyncService;
    @Resource
    private LiveDataSyncService liveDataSyncService;
    @Resource
    private OAuthTokenService oauthTokenService;
    @Autowired(required = false)
    private CrossSessionLearningService crossSessionLearningService;

    @Autowired(required = false)
    private LiveScriptAttributionService liveScriptAttributionService;

    @EventListener
    @Async
    public void onLiveSessionEnded(LiveSessionEndedEvent event) {
        LiveSession session = event.getSession();
        Long sessionId = session.getId();
        if (sessionId == null) return;

        try {
            // 1. 尝试从抖音 API 同步（需 liveUrl + accessToken）
            String roomId = extractRoomId(session.getLiveUrl());
            String accessToken = null;
            if (session.getUserId() != null) {
                accessToken = oauthTokenService.getValidAccessToken(session.getUserId(), "douyin");
            }
            if (roomId != null && accessToken != null) {
                try {
                    douyinLiveDataSyncService.syncSessionDataFromDouyin(sessionId, roomId, accessToken);
                    douyinLiveDataSyncService.syncProductDataFromDouyin(sessionId, roomId, accessToken);
                    log.info("直播场次结束，抖音数据同步完成: sessionId={}", sessionId);
                } catch (Exception e) {
                    log.warn("直播场次结束，抖音 API 同步失败（将使用 LiveMonitor 汇总）: sessionId={}", sessionId, e);
                }
            } else {
                log.debug("直播场次结束，跳过抖音 API 同步: sessionId={}, roomId={}, hasToken={}",
                        sessionId, roomId != null, accessToken != null);
            }

            // 2. 从 LiveMonitor 汇总到 live_session_data（始终执行，作为补充或主数据源）
            liveDataSyncService.syncSessionData(sessionId);
            log.debug("直播场次结束，LiveMonitor 汇总完成: sessionId={}", sessionId);

            // 3. P0-4: 跨场次学习记忆提取 — 将本场经验沉淀供下一场使用
            if (crossSessionLearningService != null) {
                try {
                    crossSessionLearningService.extractAndPersistInsights(sessionId);
                    log.debug("直播场次结束，跨场次学习记忆提取完成: sessionId={}", sessionId);
                } catch (Exception e) {
                    log.warn("直播场次结束，跨场次学习记忆提取失败（不影响主流程）: sessionId={}", sessionId, e);
                }
            }

            // 4. 话术精细归因：基于 started_at/ended_at 时间戳计算单段 GMV/互动增量
            if (liveScriptAttributionService != null) {
                try {
                    liveScriptAttributionService.persistAttributionForSession(sessionId);
                    log.debug("直播场次结束，精细话术归因完成: sessionId={}", sessionId);
                } catch (Exception e) {
                    log.warn("直播场次结束，精细话术归因失败（不影响主流程）: sessionId={}", sessionId, e);
                }
            }
        } catch (Exception e) {
            log.error("直播场次结束，后续处理异常: sessionId={}", sessionId, e);
        }
    }

    private String extractRoomId(String liveUrl) {
        if (liveUrl == null || liveUrl.isBlank()) return null;
        try {
            String[] parts = liveUrl.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            log.debug("提取 roomId 失败: liveUrl={}", liveUrl);
            return null;
        }
    }
}
