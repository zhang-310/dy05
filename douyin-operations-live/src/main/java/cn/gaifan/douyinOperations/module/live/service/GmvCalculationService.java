package cn.gaifan.douyinOperations.module.live.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

/**
 * GMV 统一计算服务。
 *
 * <h3>GMV 口径说明（全系统统一）</h3>
 * <table border="1">
 * <tr><th>场景</th><th>字段/方法</th><th>来源</th><th>更新时机</th></tr>
 * <tr><td>单场次累计 GMV（历史/复盘）</td>
 *     <td>{@link #sessionGmv}</td>
 *     <td>{@code SUM(live_product.revenue)} WHERE sessionId + deleted=0</td>
 *     <td>抖音数据同步后（场次结束）</td></tr>
 * <tr><td>实时面板成交金额</td>
 *     <td>{@code live_session_realtime_data.productPurchaseAmount}</td>
 *     <td>抖音 collector 轮询写入或弹幕 ingest</td>
 *     <td>实时（SSE 推送）</td></tr>
 * <tr><td>Dashboard 今日 GMV</td>
 *     <td>{@link #todayGmv}</td>
 *     <td>{@code SUM(live_product.revenue)} WHERE createTime >= 今日零时</td>
 *     <td>定时刷新或按需计算</td></tr>
 * <tr><td>KPI 区间 GMV</td>
 *     <td>{@link #gmvBetween}</td>
 *     <td>同上，按时间范围过滤</td>
 *     <td>按需计算</td></tr>
 * </table>
 *
 * <p><b>注意事项</b>：
 * <ul>
 *   <li>所有 GMV 均基于 {@code live_product.revenue}（排品行维度），与实时面板的 {@code productPurchaseAmount} 口径不同。</li>
 *   <li>{@code productPurchaseAmount} 是抖音实时数据，可能与 revenue 有时间差。</li>
 *   <li>礼物收入（{@code giftAmount}）不计入 GMV，仅用于实时面板展示。</li>
 *   <li>前端展示时应通过 {@code formatGmv()} 统一格式化，并附带 Tooltip 说明数据来源。</li>
 * </ul>
 */
public interface GmvCalculationService {

    /**
     * 计算单场次累计 GMV（基于 live_product.revenue 汇总）。
     *
     * @param sessionId 场次 ID
     * @return 累计 GMV（元），场次无商品时返回 ZERO
     */
    BigDecimal sessionGmv(Long sessionId);

    /**
     * 批量计算多场次 GMV（减少 N+1 查询）。
     *
     * @param sessionIds 场次 ID 列表
     * @return map: sessionId -> GMV，无商品的场次返回 ZERO
     */
    Map<Long, BigDecimal> batchSessionGmv(java.util.List<Long> sessionIds);

    /**
     * 计算今日 GMV（自然日 00:00:00 起）。
     *
     * @param orgId 机构 ID（为 null 时统计全量；管理员可传 null）
     * @return 今日 GMV（元）
     */
    BigDecimal todayGmv(Long orgId);

    /**
     * 计算指定时间区间的 GMV。
     *
     * @param orgId     机构 ID（为 null 时统计全量）
     * @param startTime 起始时间（含）
     * @param endTime   结束时间（不含）
     * @return 区间 GMV（元）
     */
    BigDecimal gmvBetween(Long orgId, Timestamp startTime, Timestamp endTime);
}
