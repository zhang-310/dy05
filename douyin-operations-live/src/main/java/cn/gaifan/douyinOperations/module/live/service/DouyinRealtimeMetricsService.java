package cn.gaifan.douyinOperations.module.live.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 抖音直播数据实时指标网关
 * <p>
 * 对接已有的 live_session_realtime_data + live_monitor 数据，
 * 计算实时 GPM/停留/互动指标，驱动 {@link LiveRealtimeStrategyService} 的话术策略切换。
 * <p>
 * 抖音直播核心流量杠杆（E-ID 模型）：
 * - GPM（千次观看成交）= 订单金额 / 观看次数 * 1000
 * - 停留时长：平均每人停留秒数
 * - 互动率：(点赞+评论) / 在线人数
 * - 转粉率：新关注 / 在线人数
 * <p>
 * 注：抖音开放平台不提供单条弹幕文本，实时指标来自内部数据积累。
 */
public interface DouyinRealtimeMetricsService {

    /**
     * 实时指标快照
     */
    record RealtimeMetrics(
            /** 场次 ID */
            Long sessionId,
            /** 实时在线人数 */
            Integer viewerCount,
            /** 累计观看人数 */
            Integer watchedCount,
            /** 当前 GPM（千次观看成交额，元） */
            BigDecimal currentGpm,
            /** 近5分钟互动率 */
            double recentInteractionRate,
            /** 近5分钟转粉率 */
            double recentFollowerRate,
            /** 在线人数趋势（相对于上一个5分钟：正=增长，负=下降） */
            double viewerTrend,
            /** 当前总 GMV */
            BigDecimal totalGmv,
            /** 高 GPM 商品列表（按当前 GPM 降序） */
            List<ProductGpm> topGpmProducts,
            /** 数据采集时间（毫秒时间戳） */
            long collectedAt
    ) {}

    /**
     * 商品 GPM 信息
     */
    record ProductGpm(Long productId, String productName, BigDecimal gpm, Integer orders) {}

    /**
     * 获取场次当前实时指标
     *
     * @param sessionId 直播场次 ID
     * @return 实时指标，数据不足时返回含零值的默认对象
     */
    RealtimeMetrics getMetrics(Long sessionId);

    /**
     * 根据实时指标判断当前是否处于流量波峰
     * <p>
     * 条件：在线人数 > 近期均值 120%，且互动率 > 阈值
     *
     * @param metrics 实时指标
     * @return true 表示波峰期
     */
    boolean isTrafficPeak(RealtimeMetrics metrics);

    /**
     * 根据实时 GPM 推荐下一个应讲解的商品
     * <p>
     * 策略：波峰期推高 GPM 商品；波谷期推低价品保转化
     *
     * @param sessionId 直播场次 ID
     * @param metrics   实时指标
     * @return 推荐商品 ID，null 表示无推荐
     */
    Long recommendNextProduct(Long sessionId, RealtimeMetrics metrics);
}
