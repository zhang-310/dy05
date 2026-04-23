package cn.gaifan.douyinOperations.module.douyinapi.collector;

import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveDataSyncService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抖音直播数据自动采集器
 * 定时采集进行中的直播数据，直播结束后全量同步
 */
@Component
@ConditionalOnProperty(name = "douyin.api.collector.enabled", havingValue = "true", matchIfMissing = false)
public class DouyinDataCollector {

    private static final Logger log = LoggerFactory.getLogger(DouyinDataCollector.class);

    @Resource
    private DouyinApiClient douyinApiClient;

    @Resource
    private OAuthTokenService oauthTokenService;

    @Resource
    private LiveSessionRepository sessionRepository;

    @Resource
    private LiveMonitorRepository monitorRepository;

    @Resource
    private LiveProductRepository productRepository;

    @Resource
    private LiveDataSyncService dataSyncService;

    /** 连续全部失败的采集轮次计数，用于静默故障告警 */
    private final AtomicInteger consecutiveFullFailures = new AtomicInteger(0);
    private static final int ALERT_THRESHOLD = 3;

    /**
     * 定时采集直播数据（每5分钟）
     * 仅采集状态为"进行中"的直播场次
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void collectLiveData() {
        log.info("开始采集直播数据...");
        List<LiveSession> liveSessions = sessionRepository.findByStatusAndDeleted(1, 0, org.springframework.data.domain.Pageable.unpaged()).getContent(); // 1=进行中

        if (liveSessions.isEmpty()) {
            log.debug("当前无进行中的直播场次");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (LiveSession session : liveSessions) {
            try {
                if (session.getLiveUrl() == null || session.getLiveUrl().isBlank()) {
                    log.warn("直播场次 {} 缺少 liveUrl，跳过采集", session.getId());
                    continue;
                }

                // 从 liveUrl 提取 roomId（假设格式：https://live.douyin.com/123456）
                String roomId = extractRoomId(session.getLiveUrl());
                if (roomId == null) {
                    log.warn("无法从 liveUrl 提取 roomId: {}", session.getLiveUrl());
                    continue;
                }

                // 获取 access_token（假设存储在 session 的某个字段，或从用户表获取）
                String accessToken = getAccessToken(session);
                if (accessToken == null) {
                    log.warn("直播场次 {} 缺少 access_token，跳过采集", session.getId());
                    continue;
                }

                // 调用抖音 API 获取实时数据
                DouyinApiClient.LiveDataResponse data = douyinApiClient.getLiveData(roomId, accessToken);
                if (data == null) {
                    log.warn("获取直播数据失败: sessionId={}, roomId={}", session.getId(), roomId);
                    failCount++;
                    continue;
                }

                // 保存到 live_monitor
                LiveMonitor monitor = new LiveMonitor();
                monitor.setSessionId(session.getId());
                monitor.setTimestamp(new Timestamp(System.currentTimeMillis()));
                monitor.setViewers(data.viewers());
                monitor.setLikes(data.likes());
                monitor.setComments(data.comments());
                monitor.setShares(data.shares());
                monitor.setProductImpressions(0); // 抖音 API 可能不提供此字段
                monitorRepository.save(monitor);

                // 更新 session 的实时数据
                session.setViewers(data.viewers());
                session.setLikes(data.likes());
                sessionRepository.save(session);

                // 采集商品数据
                collectProductData(session.getId(), roomId, accessToken);

                successCount++;
                log.debug("采集直播数据成功: sessionId={}, viewers={}, likes={}",
                        session.getId(), data.viewers(), data.likes());

            } catch (Exception e) {
                log.error("采集直播数据异常: sessionId={}", session.getId(), e);
                failCount++;
            }
        }

        log.info("直播数据采集完成: 成功={}, 失败={}", successCount, failCount);

        // 静默故障告警：连续多轮全部失败时升级日志级别
        if (successCount == 0 && failCount > 0) {
            int streak = consecutiveFullFailures.incrementAndGet();
            if (streak >= ALERT_THRESHOLD) {
                log.error("[ALERT] 直播数据采集连续 {} 轮全部失败，请检查抖音 API 连通性或 Token 状态", streak);
            }
        } else {
            consecutiveFullFailures.set(0);
        }
    }

    /**
     * 采集商品数据
     */
    private void collectProductData(Long sessionId, String roomId, String accessToken) {
        try {
            DouyinApiClient.ProductListResponse response = douyinApiClient.getProductList(roomId, accessToken);
            if (response == null || response.products() == null) {
                return;
            }

            for (DouyinApiClient.ProductInfo productInfo : response.products()) {
                // 查找或创建 LiveProduct
                LiveProduct liveProduct = productRepository.findBySessionIdAndProductId(sessionId,
                        Long.parseLong(productInfo.productId()))
                        .orElseGet(() -> {
                            LiveProduct newProduct = new LiveProduct();
                            newProduct.setSessionId(sessionId);
                            newProduct.setProductId(Long.parseLong(productInfo.productId()));
                            newProduct.setProductName(productInfo.name());
                            return newProduct;
                        });

                // 更新销量和销售额
                liveProduct.setSaleQuantity(productInfo.sales());
                liveProduct.setRevenue(BigDecimal.valueOf(productInfo.price() * productInfo.sales()));
                productRepository.save(liveProduct);
            }

            log.debug("采集商品数据成功: sessionId={}, 商品数={}", sessionId, response.products().size());
        } catch (Exception e) {
            log.error("采集商品数据失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 直播结束后全量同步
     * 监听直播状态变更为"已结束"时触发
     */
    @Async
    public void syncAfterLiveEnd(Long sessionId) {
        log.info("开始直播结束后全量同步: sessionId={}", sessionId);
        try {
            LiveSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null) {
                log.warn("直播场次不存在: sessionId={}", sessionId);
                return;
            }

            String roomId = extractRoomId(session.getLiveUrl());
            String accessToken = getAccessToken(session);

            if (roomId != null && accessToken != null) {
                // 最后一次采集数据
                DouyinApiClient.LiveDataResponse data = douyinApiClient.getLiveData(roomId, accessToken);
                if (data != null) {
                    LiveMonitor monitor = new LiveMonitor();
                    monitor.setSessionId(sessionId);
                    monitor.setTimestamp(new Timestamp(System.currentTimeMillis()));
                    monitor.setViewers(data.viewers());
                    monitor.setLikes(data.likes());
                    monitor.setComments(data.comments());
                    monitor.setShares(data.shares());
                    monitorRepository.save(monitor);
                }

                // 最后一次采集商品数据
                collectProductData(sessionId, roomId, accessToken);
            }

            // 触发数据同步（汇总到 live_session_data）
            dataSyncService.syncSessionData(sessionId);

            log.info("直播结束后全量同步完成: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("直播结束后全量同步失败: sessionId={}", sessionId, e);
        }
    }

    // ─── 工具方法 ──────────────────────────────────────

    /**
     * 从 liveUrl 提取 roomId
     * 示例: https://live.douyin.com/123456 -> 123456
     */
    private String extractRoomId(String liveUrl) {
        if (liveUrl == null || liveUrl.isBlank()) {
            return null;
        }
        try {
            String[] parts = liveUrl.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            log.error("提取 roomId 失败: liveUrl={}", liveUrl, e);
            return null;
        }
    }

    /**
     * 获取 access_token
     * 从数据库获取用户的有效 access_token（自动刷新）
     */
    private String getAccessToken(LiveSession session) {
        if (session.getUserId() == null) {
            log.warn("直播场次 {} 缺少 userId", session.getId());
            return null;
        }

        String accessToken = oauthTokenService.getValidAccessToken(session.getUserId(), "douyin");
        if (accessToken == null) {
            log.warn("用户 {} 未授权抖音或 token 已失效", session.getUserId());
        }
        return accessToken;
    }
}
