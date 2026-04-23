package cn.gaifan.douyinOperations.module.dashboard.service.impl;

import cn.gaifan.douyinOperations.module.dashboard.service.CockpitExportService;
import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitExportRequestVO;
import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitExportResultVO;
import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitPreviewVO;
import cn.gaifan.douyinOperations.module.dashboard.vo.CockpitSessionRowVO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CockpitExportServiceImpl implements CockpitExportService {

    private static final int MAX_ROWS = 5000;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<CockpitSessionRowVO> listCockpitSessions(CockpitExportRequestVO request, List<Long> visibleUserIds) {
        if (visibleUserIds != null && visibleUserIds.isEmpty()) {
            return List.of();
        }
        Query q = buildQuery(request, visibleUserIds);
        List<Object[]> raw = q.getResultList();
        List<CockpitSessionRowVO> out = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            out.add(CockpitSessionRowVO.builder()
                    .sessionId(toLong(row[0]))
                    .liveTitle(row[1] != null ? row[1].toString() : null)
                    .accountId(toLong(row[2]))
                    .userId(toLong(row[3]))
                    .status(toInt(row[4]))
                    .startTime(toLdt(row[5]))
                    .endTime(toLdt(row[6]))
                    .gmv(toBd(row[7]))
                    .productLineCount(toLong(row[8]))
                    .build());
        }
        return out;
    }

    @Override
    public CockpitExportResultVO exportSessions(CockpitExportRequestVO request, List<Long> visibleUserIds) {
        List<CockpitSessionRowVO> rows = listCockpitSessions(request, visibleUserIds);
        StringBuilder csv = new StringBuilder();
        csv.append("session_id,live_title,account_id,user_id,status,start_time,end_time,gmv,product_line_count\n");
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (CockpitSessionRowVO r : rows) {
            List<String> cols = new ArrayList<>();
            cols.add(csvEscape(r.getSessionId()));
            cols.add(csvEscape(r.getLiveTitle()));
            cols.add(csvEscape(r.getAccountId()));
            cols.add(csvEscape(r.getUserId()));
            cols.add(csvEscape(r.getStatus()));
            cols.add(r.getStartTime() != null ? csvEscape(r.getStartTime().format(iso)) : "");
            cols.add(r.getEndTime() != null ? csvEscape(r.getEndTime().format(iso)) : "");
            cols.add(csvEscape(r.getGmv()));
            cols.add(csvEscape(r.getProductLineCount()));
            csv.append(String.join(",", cols)).append('\n');
        }
        return CockpitExportResultVO.builder()
                .csv(csv.toString())
                .filename(buildFilename())
                .rowCount(rows.size())
                .build();
    }

    @Override
    public CockpitPreviewVO previewSessions(CockpitExportRequestVO request, List<Long> visibleUserIds) {
        List<CockpitSessionRowVO> rows = listCockpitSessions(request, visibleUserIds);
        return CockpitPreviewVO.builder().rows(rows).rowCount(rows.size()).build();
    }

    private Query buildQuery(CockpitExportRequestVO request, List<Long> visibleUserIds) {
        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT s.id, s.live_title, s.account_id, s.user_id, s.status,
                       s.start_time, s.end_time,
                       COALESCE((SELECT SUM(lp.revenue) FROM live_product lp WHERE lp.session_id = s.id AND lp.deleted = 0), 0),
                       COALESCE((SELECT COUNT(lp.id) FROM live_product lp WHERE lp.session_id = s.id AND lp.deleted = 0), 0)
                FROM live_session s
                WHERE s.deleted = 0
                """);

        if (visibleUserIds != null) {
            sql.append(" AND s.user_id IN (:uids) ");
        }
        if (request.getAccountId() != null) {
            sql.append(" AND s.account_id = :accountId ");
        }
        if (request.getSessionStatus() != null) {
            sql.append(" AND s.status = :sessionStatus ");
        }
        if (request.getDateFrom() != null) {
            sql.append(" AND COALESCE(s.start_time, s.create_time) >= :fromTs ");
        }
        if (request.getDateTo() != null) {
            sql.append(" AND COALESCE(s.start_time, s.create_time) < :toTsExclusive ");
        }
        String bucket = request.getHourBucket();
        if (bucket != null && !bucket.isBlank() && !"all".equalsIgnoreCase(bucket.trim())) {
            String b = bucket.trim().toLowerCase();
            switch (b) {
                case "morning" -> sql.append(" AND EXTRACT(HOUR FROM COALESCE(s.start_time, s.create_time)) BETWEEN 6 AND 11 ");
                case "afternoon" -> sql.append(" AND EXTRACT(HOUR FROM COALESCE(s.start_time, s.create_time)) BETWEEN 12 AND 17 ");
                case "evening" -> sql.append(" AND EXTRACT(HOUR FROM COALESCE(s.start_time, s.create_time)) BETWEEN 18 AND 23 ");
                case "night" -> sql.append(" AND EXTRACT(HOUR FROM COALESCE(s.start_time, s.create_time)) BETWEEN 0 AND 5 ");
                default -> { /* ignore unknown */ }
            }
        }
        if (request.getProductCategory() != null && !request.getProductCategory().isBlank()) {
            sql.append("""
                     AND EXISTS (
                       SELECT 1 FROM live_product lp2
                       JOIN dy_product dp ON dp.id = lp2.product_id AND dp.deleted = 0
                       WHERE lp2.session_id = s.id AND lp2.deleted = 0
                         AND LOWER(dp.product_category) LIKE LOWER(:catLike)
                     )
                    """);
        }

        sql.append(" ORDER BY COALESCE(s.start_time, s.create_time) DESC NULLS LAST LIMIT ").append(MAX_ROWS);

        Query q = entityManager.createNativeQuery(sql.toString());
        if (visibleUserIds != null) {
            q.setParameter("uids", visibleUserIds);
        }
        if (request.getAccountId() != null) {
            q.setParameter("accountId", request.getAccountId());
        }
        if (request.getSessionStatus() != null) {
            q.setParameter("sessionStatus", request.getSessionStatus());
        }
        if (request.getDateFrom() != null) {
            q.setParameter("fromTs", Timestamp.valueOf(request.getDateFrom().atStartOfDay()));
        }
        if (request.getDateTo() != null) {
            q.setParameter("toTsExclusive", Timestamp.valueOf(request.getDateTo().plusDays(1).atStartOfDay()));
        }
        if (request.getProductCategory() != null && !request.getProductCategory().isBlank()) {
            String c = request.getProductCategory().trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            q.setParameter("catLike", "%" + c + "%");
        }
        return q;
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(v.toString());
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(v.toString());
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal bd) {
            return bd;
        }
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(v.toString());
    }

    private static java.time.LocalDateTime toLdt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return null;
    }

    private static String buildFilename() {
        return "cockpit-sessions-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) + ".csv";
    }

    private static String csvEscape(Object v) {
        if (v == null) {
            return "";
        }
        String s = v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
