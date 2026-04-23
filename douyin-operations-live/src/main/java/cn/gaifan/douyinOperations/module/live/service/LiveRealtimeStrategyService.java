package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionRealtimeDataVO;

import java.util.List;

/**
 * 实时话术策略引擎
 * <p>
 * 综合弹幕情绪、在线人数趋势、当前话术类型，输出最优下一段话术策略建议。
 * 对应抖音 FEED 流算法的「每 5 分钟重新评估」机制，实现动态话术策略切换：
 * <ul>
 *   <li>人气上升期 → 推荐高转化商品话术</li>
 *   <li>弹幕负面爆发 → 推荐价值观/安抚/互动话术</li>
 *   <li>人气波谷 → 推荐留人/悬念/互动话术</li>
 *   <li>连续商品话术过久 → 推荐过渡/聊天话术</li>
 * </ul>
 */
public interface LiveRealtimeStrategyService {

    /**
     * 话术策略建议
     */
    record StrategyRecommendation(
            /** 建议的下一段话术类型 */
            String suggestedScriptType,
            /** 建议标签（如「人气上升，切爆品」） */
            String label,
            /** 详细说明 */
            String description,
            /** 紧急程度：normal / warning / urgent */
            String urgency,
            /** 建议依据（数据支撑） */
            String reasoning
    ) {}

    /**
     * 根据实时信号计算最优话术策略
     *
     * @param sessionId        直播场次 ID
     * @param realtimeData     当前实时数据快照（在线、点赞、互动等）
     * @param sentimentSnapshot 弹幕情绪窗口快照
     * @param currentScriptType 当前正在播出的话术类型（null 表示未知）
     * @param consecutiveSameTypeCount 连续同类型话术已执行段数（用于防重复）
     * @return 策略建议列表，按优先级排序
     */
    List<StrategyRecommendation> computeStrategy(
            Long sessionId,
            LiveSessionRealtimeDataVO realtimeData,
            DanmakuSentimentSnapshotVO sentimentSnapshot,
            String currentScriptType,
            int consecutiveSameTypeCount
    );

    /**
     * 快速判断当前是否处于流量波峰（在线人数高于近期均值的 120%）
     */
    boolean isTrafficPeak(Long sessionId, LiveSessionRealtimeDataVO realtimeData);

    /**
     * 获取当前场次建议（SSE 推送版本，每次调用自动聚合所有可用实时信号）
     */
    List<StrategyRecommendation> getSessionStrategy(Long sessionId);
}
