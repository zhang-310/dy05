package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.entity.LiveProductData;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionRealtimeData;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRealtimeDataRepository;
import cn.gaifan.douyinOperations.module.live.service.DouyinRealtimeMetricsService;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 抖音直播实时指标服务实现
 * <p>
 * 数据来源优先级：
 * 1. live_session_realtime_data（实时推入的弹幕/在线数据）
 * 2. live_monitor（定时采集的监控快照）
 * 3. live_product_data（商品维度的交易数据）
 * <p>
 * GPM 计算方式：sum(product.revenue) / watchedCount * 1000
 */
@Service
public class DouyinRealtimeMetricsServiceImpl implements DouyinRealtimeMetricsService {

    private static final Logger log = LoggerFactory.getLogger(DouyinRealtimeMetricsServiceImpl.class);

    @Resource
    private LiveSessionRealtimeDataRepository realtimeDataRepository;

    @Resource
    private LiveMonitorRepository monitorRepository;

    @Resource
    private LiveProductDataRepository productDataRepository;

    @Resource
    private DyProductRepository productRepository;

    /** 波峰判断：在线人数超过近期均值的此倍数视为波峰 */
    @Value("${app.live.realtime-strategy.peak-ratio:1.2}")
    private double peakRatio;

    /** 互动率阈值（波峰额外条件） */
    @Value("${app.live.realtime-strategy.low-interaction-rate:0.03}")
    private double interactionRateThreshold;

    @Override
    public RealtimeMetrics getMetrics(Long sessionId) {
        if (sessionId == null) return emptyMetrics(sessionId);

        try {
            // 1. 实时数据
            LiveSessionRealtimeData rtData = realtimeDataRepository.findByLiveSessionId(sessionId).orElse(null);

            int viewerCount = rtData != null && rtData.getViewerCount() != null ? rtData.getViewerCount() : 0;
            int watchedCount = rtData != null && rtData.getWatchedCount() != null ? rtData.getWatchedCount() : 0;
            int likeCount = rtData != null && rtData.getLikeCount() != null ? rtData.getLikeCount() : 0;
            int commentCount = rtData != null && rtData.getCommentCount() != null ? rtData.getCommentCount() : 0;
            int followCount = rtData != null && rtData.getFollowCount() != null ? rtData.getFollowCount() : 0;

            // 2. 商品 GMV 数据
            List<LiveProductData> productDataList = productDataRepository.findBySessionId(sessionId);
            BigDecimal totalGmv = productDataList.stream()
                    .map(p -> p.getRevenue() != null ? p.getRevenue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 3. 计算 GPM
            BigDecimal gpm = BigDecimal.ZERO;
            if (watchedCount > 0 && totalGmv.compareTo(BigDecimal.ZERO) > 0) {
                gpm = totalGmv.multiply(BigDecimal.valueOf(1000))
                        .divide(BigDecimal.valueOf(watchedCount), 2, RoundingMode.HALF_UP);
            }

            // 4. 互动率（近实时）
            double interactionRate = viewerCount > 0
                    ? (double)(likeCount + commentCount) / viewerCount : 0;
            double followerRate = viewerCount > 0 ? (double) followCount / viewerCount : 0;

            // 5. 在线趋势（与最近10条 monitor 记录对比）
            double viewerTrend = computeViewerTrend(sessionId, viewerCount);

            // 6. 高 GPM 商品列表
            List<ProductGpm> topGpmProducts = buildTopGpmProducts(sessionId, productDataList, watchedCount);

            return new RealtimeMetrics(
                    sessionId, viewerCount, watchedCount, gpm, interactionRate, followerRate,
                    viewerTrend, totalGmv, topGpmProducts, System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.debug("[DouyinMetrics] session={} 指标计算失败: {}", sessionId, e.getMessage());
            return emptyMetrics(sessionId);
        }
    }

    @Override
    public boolean isTrafficPeak(RealtimeMetrics metrics) {
        if (metrics == null || metrics.viewerCount() == null || metrics.viewerCount() < 50) return false;
        // 条件1：在线人数增长超过 peakRatio 倍
        boolean growing = metrics.viewerTrend() > (peakRatio - 1.0);
        // 条件2：互动率正常（避免刷量情况）
        boolean interacting = metrics.recentInteractionRate() >= interactionRateThreshold;
        return growing && interacting;
    }

    @Override
    public Long recommendNextProduct(Long sessionId, RealtimeMetrics metrics) {
        if (metrics == null || metrics.topGpmProducts().isEmpty()) return null;
        boolean isPeak = isTrafficPeak(metrics);
        if (isPeak) {
            // 波峰期：推 GPM 最高的商品
            return metrics.topGpmProducts().get(0).productId();
        } else {
            // 波谷期：推 GPM 相对低但有订单的商品（促进转化）
            return metrics.topGpmProducts().stream()
                    .filter(p -> p.orders() != null && p.orders() > 0)
                    .min(Comparator.comparing(ProductGpm::gpm))
                    .map(ProductGpm::productId)
                    .orElse(metrics.topGpmProducts().get(0).productId());
        }
    }

    // ─── 私有方法 ─────────────────────────────────────────────────────────────────

    private double computeViewerTrend(Long sessionId, int currentViewerCount) {
        try {
            // 取最近 10 条 monitor 记录计算均值
            List<LiveMonitor> recent = monitorRepository.findBySessionIdOrderByTimestampAsc(sessionId)
                    .stream().filter(m -> m.getOnlineCount() != null && m.getOnlineCount() > 0)
                    .toList();
            if (recent.size() < 2) return 0;
            // 取倒数第6条到倒数第11条（即5分钟前的数据）作为基线
            int baselineStart = Math.max(0, recent.size() - 11);
            int baselineEnd = Math.max(0, recent.size() - 6);
            if (baselineEnd <= baselineStart) return 0;
            double baselineAvg = recent.subList(baselineStart, baselineEnd).stream()
                    .mapToInt(m -> m.getOnlineCount())
                    .average().orElse(0);
            return baselineAvg > 0 ? (currentViewerCount - baselineAvg) / baselineAvg : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<ProductGpm> buildTopGpmProducts(Long sessionId, List<LiveProductData> productDataList, int watchedCount) {
        List<ProductGpm> result = new ArrayList<>();
        for (LiveProductData pd : productDataList) {
            if (pd.getRevenue() == null || pd.getRevenue().compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal pGpm = watchedCount > 0
                    ? pd.getRevenue().multiply(BigDecimal.valueOf(1000))
                            .divide(BigDecimal.valueOf(watchedCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String productName = productRepository.findById(pd.getProductId())
                    .map(p -> p.getProductName()).orElse("未知商品");
            result.add(new ProductGpm(pd.getProductId(), productName, pGpm,
                    pd.getOrders() != null ? pd.getOrders() : 0));
        }
        result.sort(Comparator.comparing(ProductGpm::gpm).reversed());
        return result.size() > 5 ? result.subList(0, 5) : result;
    }

    private RealtimeMetrics emptyMetrics(Long sessionId) {
        return new RealtimeMetrics(sessionId, 0, 0, BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO, List.of(), System.currentTimeMillis());
    }
}
