package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvGenerationLog;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvGenerationLogRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.QualityDashboardService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 质量仪表板服务实现 (Phase 6.3)
 */
@Service
public class QualityDashboardServiceImpl implements QualityDashboardService {

    private static final Logger log = LoggerFactory.getLogger(QualityDashboardServiceImpl.class);

    @Resource
    private SvGenerationLogRepository generationLogRepository;

    @Override
    public Map<String, Object> getOverview(Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            LocalDate now = LocalDate.now();
            LocalDate weekStart = now.minusDays(7);
            Timestamp start = Timestamp.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Timestamp end = Timestamp.from(now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<SvGenerationLog> logs = generationLogRepository.findByOwnerIdAndCreateTimeBetweenOrderByCreateTimeAsc(ownerId, start, end);
        int total = logs.size();
        long successCount = logs.stream().filter(l -> Boolean.TRUE.equals(l.getSuccess())).count();
        double avgScore = logs.stream()
            .filter(l -> l.getQualityScore() != null)
            .mapToDouble(l -> l.getQualityScore().doubleValue())
            .average()
            .orElse(0);

        String bestModel = null;
        double bestAvg = 0;
        for (Object[] row : generationLogRepository.avgQualityByProvider(ownerId, start, end)) {
            String provider = (String) row[0];
            BigDecimal avg = (BigDecimal) row[1];
            if (avg != null && avg.doubleValue() > bestAvg) {
                bestAvg = avg.doubleValue();
                bestModel = provider != null ? provider : "ffmpeg";
            }
        }
        if (bestModel == null && !logs.isEmpty()) bestModel = "ffmpeg";

        return Map.of(
            "publishCount", total,
            "successRate", total > 0 ? Math.round(successCount * 100.0 / total) : 0,
            "avgScore", Math.round(avgScore * 10) / 10.0,
            "grade", scoreToGrade(avgScore),
            "bestModel", bestModel != null ? bestModel : "-",
            "bestModelScore", Math.round(bestAvg * 10) / 10.0
        );
        } catch (Exception e) {
            log.warn("质量概览查询异常，返回空数据: {}", e.getMessage());
            return Map.of(
                "publishCount", 0,
                "successRate", 0,
                "avgScore", 0.0,
                "grade", "-",
                "bestModel", "-",
                "bestModelScore", 0.0
            );
        }
    }

    @Override
    public List<Map<String, Object>> getQualityTrend(Long ownerId, int days) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        try {
            LocalDate startDate = LocalDate.now().minusDays(days - 1);
            Timestamp start = Timestamp.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Timestamp end = Timestamp.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<Object[]> rows = generationLogRepository.dailyAggregateByOwner(ownerId, start, end);
            java.util.Map<String, Map<String, Object>> dayMap = new java.util.HashMap<>();
            for (Object[] row : rows) {
                Object dt = row[0];
                Object avgScore = row[1];
                Object cnt = row[2];
                String dateStr = dt instanceof java.sql.Date d ? d.toLocalDate().toString()
                    : dt instanceof java.time.LocalDate ld ? ld.toString() : String.valueOf(dt);
                double avg = avgScore instanceof Number n ? n.doubleValue() : 0;
                int count = cnt instanceof Number n ? n.intValue() : 0;
                dayMap.put(dateStr, Map.of("date", dateStr, "avgScore", Math.round(avg * 10) / 10.0, "count", count));
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate d = LocalDate.now().minusDays(i);
                String dateStr = d.toString();
                result.add(dayMap.getOrDefault(dateStr, Map.of("date", dateStr, "avgScore", 0.0, "count", 0)));
            }
            return result;
        } catch (Exception e) {
            log.warn("质量趋势查询异常: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getModelRanking(Long ownerId, int days) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        try {
        LocalDate startDate = LocalDate.now().minusDays(days);
        Timestamp start = Timestamp.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp end = Timestamp.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : generationLogRepository.avgQualityByProvider(ownerId, start, end)) {
            String provider = (String) row[0];
            BigDecimal avg = (BigDecimal) row[1];
            result.add(Map.of(
                "rank", rank++,
                "model", provider != null ? provider : "ffmpeg",
                "avgScore", avg != null ? Math.round(avg.doubleValue() * 10) / 10.0 : 0
            ));
        }
        if (result.isEmpty()) {
            result.add(Map.of("rank", 1, "model", "-", "avgScore", 0));
        }
        return result;
        } catch (Exception e) {
            log.warn("模型排名查询异常: {}", e.getMessage());
            return List.of(Map.of("rank", 1, "model", "-", "avgScore", 0));
        }
    }

    @Override
    public List<Map<String, Object>> getCameraRanking(Long ownerId, int days) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        try {
        LocalDate startDate = LocalDate.now().minusDays(days);
        Timestamp start = Timestamp.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Timestamp end = Timestamp.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : generationLogRepository.avgQualityByCameraType(ownerId, start, end)) {
            String camera = (String) row[0];
            BigDecimal avg = (BigDecimal) row[1];
            result.add(Map.of(
                "rank", rank++,
                "cameraType", camera != null ? camera : "-",
                "avgScore", avg != null ? Math.round(avg.doubleValue() * 10) / 10.0 : 0
            ));
        }
        if (result.isEmpty()) {
            result.add(Map.of("rank", 1, "cameraType", "-", "avgScore", 0));
        }
        return result;
        } catch (Exception e) {
            log.warn("运镜排名查询异常: {}", e.getMessage());
            return List.of(Map.of("rank", 1, "cameraType", "-", "avgScore", 0));
        }
    }

    @Override
    public List<String> getAiReflections(Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        // 占位：待接入 LLM 生成反思
        return List.of(
            "建议增加过渡镜头，提升完播率",
            "周三/周五晚8点发布效果最佳",
            "4K 级别在 MiniMax 上性价比最高"
        );
    }

    private static String scoreToGrade(double score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        return "D";
    }
}
