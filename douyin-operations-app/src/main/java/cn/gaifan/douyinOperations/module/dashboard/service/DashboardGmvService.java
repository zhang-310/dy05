package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard GMV / KPI 统计服务（D5 升级新增端点）
 */
@Service
public class DashboardGmvService {

    @Resource
    private LiveSessionRepository sessionRepository;

    @Resource
    private LiveProductRepository productRepository;

    // ===================== 统一 KPI =====================

    public Map<String, Object> getUnifiedKpi(Long userId, int lookbackDays) {
        Timestamp since = sinceTimestamp(lookbackDays);
        Timestamp todayStart = todayStartTs();

        Map<String, Object> result = new LinkedHashMap<>();

        // 内容维度
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("shortVideoCount", 0L);
        content.put("productScriptCount", 0L);
        content.put("kbDocumentCount", 0L);
        result.put("content", content);

        // 流量维度（从直播场次聚合 viewers/likes）
        long viewerSum = 0L;
        List<LiveSession> sessions = getSessionsByUser(userId, since);
        for (LiveSession s : sessions) {
            if (s.getViewers() != null) viewerSum += s.getViewers();
        }
        Map<String, Object> traffic = new LinkedHashMap<>();
        traffic.put("liveViewerSum", viewerSum);
        traffic.put("shortVideoViewSum", 0L);
        result.put("traffic", traffic);

        // 转化维度
        Map<String, Object> conversion = new LinkedHashMap<>();
        conversion.put("saleQuantitySinceToday", 0L);
        conversion.put("revenueLinesSinceToday", 0L);
        result.put("conversion", conversion);

        // 收入维度
        BigDecimal todayGmv = sumGmvByUser(userId, todayStart, null);
        Timestamp yesterdayStart = sinceTimestamp(1);
        BigDecimal yesterdayGmv = sumGmvByUser(userId, yesterdayStart, todayStart);
        Map<String, Object> revenue = new LinkedHashMap<>();
        revenue.put("todayGmv", todayGmv);
        revenue.put("yesterdayGmv", yesterdayGmv);
        revenue.put("avgOrderValueToday", BigDecimal.ZERO);
        result.put("revenue", revenue);

        return result;
    }

    // ===================== 直播形式 GMV 分布 =====================

    public Map<String, Object> getLiveFormatGmv(Long userId, int lookbackDays) {
        Timestamp since = sinceTimestamp(lookbackDays);
        List<LiveSession> sessions = getSessionsByUser(userId, since);

        // 按 sessionType（直播形式）分组聚合
        Map<String, Object> byFormat = new LinkedHashMap<>();
        Map<String, Long> countMap = new LinkedHashMap<>();
        Map<String, BigDecimal> gmvMap = new LinkedHashMap<>();

        for (LiveSession s : sessions) {
            String fmt = s.getSessionType() != null ? s.getSessionType() : "普通直播";
            countMap.merge(fmt, 1L, Long::sum);
            BigDecimal gmv = productRepository.sumRevenueBySessionId(s.getId());
            gmvMap.merge(fmt, gmv != null ? gmv : BigDecimal.ZERO, BigDecimal::add);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String fmt : countMap.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("liveFormat", fmt);
            row.put("sessionCount", countMap.get(fmt));
            row.put("totalGmv", gmvMap.getOrDefault(fmt, BigDecimal.ZERO));
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookbackDays", lookbackDays);
        result.put("since", since.toLocalDateTime().toLocalDate().toString());
        result.put("rows", rows);
        return result;
    }

    // ===================== 商品 GMV 汇总 =====================

    public Map<String, Object> getProductGmvSummary(Long userId, int lookbackDays) {
        Timestamp since = sinceTimestamp(lookbackDays);
        List<LiveSession> sessions = getSessionsByUser(userId, since);
        List<Long> sessionIds = sessions.stream().map(LiveSession::getId).collect(Collectors.toList());

        List<Map<String, Object>> rows = new ArrayList<>();
        if (!sessionIds.isEmpty()) {
            List<LiveProduct> products = new ArrayList<>();
            for (Long sid : sessionIds) {
                products.addAll(productRepository.findBySessionIdAndDeleted(sid, 0));
            }
            // 按商品聚合
            Map<Long, String> nameMap = new LinkedHashMap<>();
            Map<Long, BigDecimal> gmvSumMap = new LinkedHashMap<>();
            Map<Long, Long> countMap = new LinkedHashMap<>();
            for (LiveProduct lp : products) {
                if (lp.getProductId() == null) continue;
                nameMap.putIfAbsent(lp.getProductId(), lp.getProductName() != null ? lp.getProductName() : "商品" + lp.getProductId());
                gmvSumMap.merge(lp.getProductId(), lp.getRevenue() != null ? lp.getRevenue() : BigDecimal.ZERO, BigDecimal::add);
                countMap.merge(lp.getProductId(), 1L, Long::sum);
            }
            // 按 GMV 降序
            gmvSumMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                    .limit(20)
                    .forEach(e -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("productId", e.getKey());
                        row.put("productName", nameMap.get(e.getKey()));
                        row.put("sessionCount", countMap.getOrDefault(e.getKey(), 0L));
                        row.put("totalGmv", e.getValue());
                        rows.add(row);
                    });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookbackDays", lookbackDays);
        result.put("since", since.toLocalDateTime().toLocalDate().toString());
        result.put("rows", rows);
        return result;
    }

    // ===================== 驾驶舱预览 =====================

    public Map<String, Object> getCockpitPreview(Long userId, Map<String, Object> filters) {
        Timestamp since = sinceTimestamp(30);
        if (filters != null && filters.get("dateFrom") instanceof String dateFrom && !dateFrom.isBlank()) {
            try {
                since = Timestamp.valueOf(LocalDate.parse(dateFrom).atStartOfDay());
            } catch (Exception ignored) {}
        }

        List<LiveSession> sessions = getSessionsByUser(userId, since);

        // 状态过滤
        Integer statusFilter = null;
        if (filters != null && filters.get("sessionStatus") instanceof Number n) {
            statusFilter = n.intValue();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (LiveSession s : sessions) {
            if (statusFilter != null && !statusFilter.equals(s.getStatus())) continue;
            BigDecimal gmv = productRepository.sumRevenueBySessionId(s.getId());
            long productLineCount = productRepository.countBySessionId(s.getId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sessionId", s.getId());
            row.put("liveTitle", s.getLiveTitle());
            row.put("accountId", s.getAccountId());
            row.put("userId", s.getUserId());
            row.put("status", s.getStatus());
            row.put("startTime", s.getStartTime() != null ? s.getStartTime().toString() : null);
            row.put("endTime", s.getEndTime() != null ? s.getEndTime().toString() : null);
            row.put("gmv", gmv != null ? gmv : BigDecimal.ZERO);
            row.put("productLineCount", productLineCount);
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("rowCount", rows.size());
        return result;
    }

    // ===================== 利润矩阵预览 =====================

    public Map<String, Object> getProfitMatrixPreview(Long userId, int lookbackDays) {
        Timestamp since = sinceTimestamp(lookbackDays);
        List<LiveSession> sessions = getSessionsByUser(userId, since);

        Map<String, BigDecimal> gmvByFormat = new LinkedHashMap<>();
        Map<String, Long> countByFormat = new LinkedHashMap<>();
        for (LiveSession s : sessions) {
            String fmt = s.getSessionType() != null ? s.getSessionType() : "普通直播";
            BigDecimal gmv = productRepository.sumRevenueBySessionId(s.getId());
            gmvByFormat.merge(fmt, gmv != null ? gmv : BigDecimal.ZERO, BigDecimal::add);
            countByFormat.merge(fmt, 1L, Long::sum);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        gmvByFormat.forEach((fmt, gmv) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("liveFormat", fmt);
            row.put("sessionCount", countByFormat.getOrDefault(fmt, 0L));
            row.put("totalGmv", gmv);
            // 估算毛利率（暂用固定值，可后续接真实成本数据）
            row.put("estimatedMarginRate", new BigDecimal("0.25"));
            row.put("isEstimated", true);
            row.put("note", "毛利率为估算值，以实际结算为准");
            rows.add(row);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookbackDays", lookbackDays);
        result.put("since", since.toLocalDateTime().toLocalDate().toString());
        result.put("rows", rows);
        return result;
    }

    // ===================== 转化漏斗 =====================

    public Map<String, Object> getConversionFunnel(Long userId, int lookbackDays) {
        Timestamp since = sinceTimestamp(lookbackDays);
        List<LiveSession> sessions = getSessionsByUser(userId, since);

        long viewerTotal = sessions.stream()
                .mapToLong(s -> s.getViewers() != null ? s.getViewers() : 0)
                .sum();
        long likeTotal = sessions.stream()
                .mapToLong(s -> s.getLikes() != null ? s.getLikes() : 0)
                .sum();
        long productLines = 0L;
        for (LiveSession s : sessions) {
            productLines += productRepository.countBySessionId(s.getId());
        }

        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(buildFunnelStep("观看", viewerTotal, viewerTotal));
        steps.add(buildFunnelStep("点赞", likeTotal, viewerTotal));
        steps.add(buildFunnelStep("进入商品", productLines, viewerTotal));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookbackDays", lookbackDays);
        result.put("steps", steps);
        return result;
    }

    // ===================== 驾驶舱 CSV 导出 =====================

    public Map<String, Object> exportCockpitCsv(Long userId, Map<String, Object> filters) {
        Map<String, Object> preview = getCockpitPreview(userId, filters);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) preview.getOrDefault("rows", Collections.emptyList());

        StringBuilder sb = new StringBuilder();
        sb.append("场次ID,标题,状态,开始时间,结束时间,GMV,商品线数\n");
        for (Map<String, Object> row : rows) {
            sb.append(row.getOrDefault("sessionId", "")).append(",")
              .append(csvEscape(String.valueOf(row.getOrDefault("liveTitle", "")))).append(",")
              .append(row.getOrDefault("status", "")).append(",")
              .append(row.getOrDefault("startTime", "")).append(",")
              .append(row.getOrDefault("endTime", "")).append(",")
              .append(row.getOrDefault("gmv", "0")).append(",")
              .append(row.getOrDefault("productLineCount", "0")).append("\n");
        }

        String filename = "cockpit_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("csv", sb.toString());
        result.put("filename", filename);
        result.put("rowCount", rows.size());
        return result;
    }

    // ===================== 私有辅助方法 =====================

    private List<LiveSession> getSessionsByUser(Long userId, Timestamp since) {
        if (userId == null) return Collections.emptyList();
        // 查询 userId 对应的已结束或进行中场次
        return sessionRepository.findByUserIdAndDeleted(userId, 0,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createTime")))
                .getContent().stream()
                .filter(s -> s.getCreateTime() != null && !s.getCreateTime().before(since))
                .collect(Collectors.toList());
    }

    private BigDecimal sumGmvByUser(Long userId, Timestamp from, Timestamp to) {
        if (userId == null) return BigDecimal.ZERO;
        try {
            List<LiveSession> sessions = getSessionsByUser(userId, from);
            BigDecimal total = BigDecimal.ZERO;
            for (LiveSession s : sessions) {
                if (to != null && s.getCreateTime() != null && s.getCreateTime().after(to)) continue;
                BigDecimal gmv = productRepository.sumRevenueBySessionId(s.getId());
                if (gmv != null) total = total.add(gmv);
            }
            return total;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    private Timestamp sinceTimestamp(int lookbackDays) {
        return Timestamp.valueOf(LocalDateTime.of(
                LocalDate.now().minusDays(lookbackDays), LocalTime.MIDNIGHT));
    }

    private Timestamp todayStartTs() {
        return Timestamp.valueOf(LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT));
    }

    private Map<String, Object> buildFunnelStep(String name, long value, long base) {
        double rate = base > 0 ? (double) value / base * 100.0 : 0.0;
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", name);
        step.put("value", value);
        step.put("rate", Math.round(rate * 100.0) / 100.0);
        return step;
    }

    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
