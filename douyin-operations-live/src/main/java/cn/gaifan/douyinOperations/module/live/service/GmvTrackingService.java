package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.contract.payment.SessionCompletionRate;
import cn.gaifan.douyinOperations.contract.payment.SessionGmvSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GMV 追踪服务 — 支付↔场次关联
 *
 * Vision P0: 实现单场 GMV 计算，payment_order ↔ live_session 关联
 */
@Service
public class GmvTrackingService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询场次 GMV 汇总
     */
    public SessionGmvSummary getSessionGmv(Long sessionId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT s.id as session_id, s.live_title, s.account_id, " +
                    "COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0) as total_amount, " +
                    "COALESCE(SUM(CASE WHEN p.status = 'REFUNDED' THEN p.amount ELSE 0 END), 0) as refund_amount, " +
                    "COUNT(CASE WHEN p.status = 'PAID' THEN 1 END) as order_count, " +
                    "COUNT(CASE WHEN p.status = 'REFUNDED' THEN 1 END) as refund_count " +
                    "FROM live_session s " +
                    "LEFT JOIN payment_order p ON s.id = p.session_id AND p.deleted = 0 " +
                    "WHERE s.id = ? AND s.deleted = 0 " +
                    "GROUP BY s.id",
                    sessionId);

            Long total = toLong(row.get("total_amount"));
            Long refund = toLong(row.get("refund_amount"));
            return new SessionGmvSummary(
                    toLong(row.get("session_id")),
                    null,
                    total, refund, total - refund,
                    toInt(row.get("order_count")),
                    toInt(row.get("refund_count")),
                    toLong(row.get("account_id")),
                    (String) row.get("live_title")
            );
        } catch (Exception e) {
            return new SessionGmvSummary(sessionId, null, 0L, 0L, 0L, 0, 0, null, null);
        }
    }

    /** 最近 N 场 GMV 排行 */
    public List<SessionGmvSummary> getTopSessionsByGmv(int limit) {
        try {
            return jdbcTemplate.query(
                    "SELECT s.id as session_id, s.live_title, s.account_id, " +
                    "COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0) as total, " +
                    "COALESCE(SUM(CASE WHEN p.status = 'REFUNDED' THEN p.amount ELSE 0 END), 0) as refund " +
                    "FROM live_session s " +
                    "LEFT JOIN payment_order p ON s.id = p.session_id AND p.deleted = 0 " +
                    "WHERE s.deleted = 0 AND s.status = 2 " +
                    "GROUP BY s.id ORDER BY total DESC LIMIT ?",
                    (rs, rowNum) -> {
                        Long total = rs.getLong("total");
                        Long refund = rs.getLong("refund");
                        return new SessionGmvSummary(
                                rs.getLong("session_id"), null,
                                total, refund, total - refund,
                                0, 0,
                                rs.getLong("account_id"),
                                rs.getString("live_title")
                        );
                    }, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 完播率 — 当前从实时数据表读取，fallback 到 estimated */
    public SessionCompletionRate getCompletionRate(Long sessionId) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT avg_watch_duration, full_watch_ratio, viewer_count " +
                    "FROM live_session_realtime_data " +
                    "WHERE session_id = ? ORDER BY create_time DESC LIMIT 1",
                    sessionId);
            double ratio = row.get("full_watch_ratio") != null
                    ? ((Number) row.get("full_watch_ratio")).doubleValue() : 0.85;
            return new SessionCompletionRate(
                    sessionId,
                    row.get("avg_watch_duration") != null ? ((Number) row.get("avg_watch_duration")).doubleValue() : 0,
                    ratio,
                    row.get("viewer_count") != null ? ((Number) row.get("viewer_count")).intValue() : 0,
                    "realtime"
            );
        } catch (Exception e) {
            return SessionCompletionRate.estimated(sessionId);
        }
    }

    private static Long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof BigDecimal bd) return bd.longValue();
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        return 0;
    }
}
