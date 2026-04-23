package cn.gaifan.douyinOperations.module.live.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 直播场次只读查询服务（供跨模块消费者使用）。
 *
 * <h3>归因体系说明</h3>
 * <p>系统中存在两种语义不同的"归因"：
 * <ul>
 *   <li><b>话术效果统计</b>（场内实时）：{@link LiveScriptAttributionService}，
 *       基于 LiveMonitor 时序数据计算执行后 30s 观众变化和互动转化，
 *       产出 {@code live_script.effectiveness_score}。
 *       适用场景：单场话术排名、播中实时反馈。</li>
 *   <li><b>LLM 因果归因报告</b>（离线分析）：{@code module/attribution/AttributionService}，
 *       基于场次全量数据触发 LLM 生成"哪些话术/商品驱动了 GMV"的可解释报告，
 *       产出 {@code attribution} 实体。
 *       适用场景：场次复盘、跨场次策略建议。</li>
 * </ul>
 *
 * <p>外部模块（dashboard、attribution、payment 等）应通过此接口访问直播场次数据，
 * 而非直接依赖 {@code LiveSession} Entity 或 {@code LiveSessionRepository}，
 * 以降低跨模块耦合度。
 */
public interface LiveSessionReadService {

    /** 场次基础信息视图（面向跨模块消费）*/
    record SessionSummary(
            Long id,
            Long userId,
            Long orgId,
            Long accountId,
            String title,
            Integer status,
            String liveFormat,
            String sessionType,
            BigDecimal cumulativeGmv,
            Integer productCount,
            Integer scriptCount,
            Timestamp startTime,
            Timestamp endTime,
            Timestamp createTime
    ) {}

    /**
     * 按 ID 获取场次基础摘要（不含 Entity 引用）。
     */
    Optional<SessionSummary> findSummaryById(Long sessionId);

    /**
     * 查询用户的场次摘要列表（分页）。
     *
     * @param userId   用户 ID（必填，做数据隔离）
     * @param page     页码（0-indexed）
     * @param pageSize 页大小（最大 100）
     * @return 场次摘要列表
     */
    List<SessionSummary> findSummariesByUserId(Long userId, int page, int pageSize);

    /**
     * 统计各状态的场次数（用于 Dashboard 聚合）。
     *
     * @param orgId 机构 ID（为 null 时查询全局）
     * @return map: status -> count
     */
    java.util.Map<Integer, Long> countByStatus(Long orgId);

    /**
     * 统计指定日期范围内的 GMV 总额（用于 Dashboard/KPI）。
     *
     * @param orgId     机构 ID（为 null 时查询全局）
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（含）
     */
    BigDecimal sumGmvByDateRange(Long orgId, Timestamp startTime, Timestamp endTime);
}
