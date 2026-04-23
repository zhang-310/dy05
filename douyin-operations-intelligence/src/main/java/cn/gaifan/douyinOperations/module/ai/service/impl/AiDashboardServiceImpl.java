package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallQuotaRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeQualityScoreRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiDashboardService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiDashboardServiceImpl implements AiDashboardService {

    @Resource
    private AiCallLogRepository callLogRepository;

    @Resource
    private AiCallQuotaRepository quotaRepository;

    @Autowired(required = false)
    private KnowledgeQualityScoreRepository qualityScoreRepository;

    @Override
    public List<Map<String, Object>> getCallVolumeTrend(int days, String callType) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        Timestamp startTs = Timestamp.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Timestamp endTs = Timestamp.from(end.atZone(ZoneId.systemDefault()).toInstant());

        List<Object[]> rows = callType == null || callType.isBlank()
                ? callLogRepository.countByDateRange(startTs, endTs)
                : callLogRepository.countByDateRangeAndCallType(startTs, endTs, callType);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row[0] != null ? row[0].toString() : null);
            m.put("count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getCallVolumeTrendByHour(int hours, String callType) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(hours);
        Timestamp startTs = Timestamp.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Timestamp endTs = Timestamp.from(end.atZone(ZoneId.systemDefault()).toInstant());

        List<Object[]> rows = callLogRepository.countByHourRange(startTs, endTs, callType == null || callType.isBlank() ? null : callType);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row[0] != null ? row[0].toString() : null);
            m.put("count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getQuotaTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        Date startDate = Date.valueOf(start);
        Date endDate = Date.valueOf(end);

        List<Object[]> rows = quotaRepository.aggregateByDateRange(startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", row[0] != null ? row[0].toString() : null);
            m.put("usedCount", row[1] != null ? ((Number) row[1]).intValue() : 0);
            m.put("maxCount", row[2] != null ? ((Number) row[2]).intValue() : 0);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getCallTypeDistribution(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        Timestamp startTs = Timestamp.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Timestamp endTs = Timestamp.from(end.atZone(ZoneId.systemDefault()).toInstant());

        List<Object[]> rows = callLogRepository.countByCallType(startTs, endTs);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("callType", row[0]);
            m.put("count", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 今日起始时间
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Timestamp todayTs = Timestamp.from(todayStart.atZone(ZoneId.systemDefault()).toInstant());

        // 本月起始时间
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Timestamp monthTs = Timestamp.from(monthStart.atZone(ZoneId.systemDefault()).toInstant());

        // 今日调用次数
        long todayCalls = callLogRepository.countByCreateTimeAfterAndStatus(todayTs, 1);
        stats.put("todayCalls", todayCalls);

        // 本月 Token 消耗
        long monthTokens = callLogRepository.sumTotalTokensSince(monthTs);
        stats.put("monthTokens", monthTokens);

        // 今日成功率
        long todaySuccess = callLogRepository.countSuccessCallsSince(todayTs);
        long todayFailed = callLogRepository.countFailedCallsSince(todayTs);
        long todayTotal = todaySuccess + todayFailed;
        double successRate = todayTotal > 0 ? (todaySuccess * 100.0 / todayTotal) : 100.0;
        stats.put("successRate", Math.round(successRate * 10) / 10.0);

        // 知识库质量均分
        double qualityScore = 0.0;
        if (qualityScoreRepository != null) {
            BigDecimal avg = qualityScoreRepository.calculateGlobalAverageQualityScore();
            qualityScore = avg != null ? avg.doubleValue() : 0.0;
        }
        stats.put("avgQualityScore", Math.round(qualityScore * 10) / 10.0);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getCostBreakdown(int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        Timestamp startTs = Timestamp.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Timestamp endTs = Timestamp.from(end.atZone(ZoneId.systemDefault()).toInstant());

        List<Object[]> rows = callLogRepository.aggregateTokensByCallType(startTs, endTs);
        long totalTokens = 0L;
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            String ct = row[0] != null ? row[0].toString() : "unknown";
            long tokens = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long calls = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            totalTokens += tokens;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("callType", ct);
            m.put("tokens", tokens);
            m.put("calls", calls);
            items.add(m);
        }
        for (Map<String, Object> m : items) {
            long t = ((Number) m.get("tokens")).longValue();
            m.put("tokenSharePct", totalTokens > 0 ? Math.round(t * 1000.0 / totalTokens) / 10.0 : 0.0);
        }
        items.sort(Comparator.comparingLong(m -> -((Number) m.get("tokens")).longValue()));
        return items;
    }
}
