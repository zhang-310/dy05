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
        try {
            LocalDate now = LocalDate.now();
            Timestamp start = Timestamp.from(now.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Timestamp end = Timestamp.from(now.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            List<SvGenerationLog> logs = generationLogRepository.findByOwnerIdAndCreateTimeBetweenOrderByCreateTimeAsc(ownerId, start, end);
            if (logs.isEmpty()) {
                return List.of("近 7 天暂无生成日志，建议先完成成片生成并让质量评分落库，再查看 AI 反思。");
            }

            int total = logs.size();
            long successCount = logs.stream().filter(l -> Boolean.TRUE.equals(l.getSuccess())).count();
            double successRate = successCount * 100.0 / total;
            double avgScore = logs.stream()
                .filter(l -> l.getQualityScore() != null)
                .mapToDouble(l -> l.getQualityScore().doubleValue())
                .average()
                .orElse(0);
            long scoredCount = logs.stream().filter(l -> l.getQualityScore() != null).count();
            long slowCount = logs.stream()
                .filter(l -> l.getGenerationTimeMs() != null && l.getGenerationTimeMs() > 120_000)
                .count();
            long noAudioCount = logs.stream().filter(l -> !Boolean.TRUE.equals(l.getHasAudio())).count();

            List<String> result = new ArrayList<>();
            result.add(String.format("近 7 天生成 %d 次，成功率 %.1f%%，平均质量分 %.1f（有评分 %d 条）。",
                total, successRate, avgScore, scoredCount));
            if (successRate < 90) {
                result.add("生成成功率低于 90%，请优先排查失败日志中的模型调用、素材 URL 和回调状态。");
            }
            if (scoredCount == 0) {
                result.add("当前生成日志没有质量分，建议先补齐质量评分任务，否则模型/运镜排名和反思都只能降级。");
            } else if (avgScore < 75) {
                result.add("平均质量分低于 75，建议复查分镜提示词、关键帧一致性和成片质检规则。");
            } else {
                result.add("平均质量分已达可用区间，建议保留高分模型和高分运镜组合做模板复用。");
            }

            bestProviderReflection(ownerId, start, end).ifPresent(result::add);
            bestCameraReflection(ownerId, start, end).ifPresent(result::add);
            if (slowCount > 0) {
                result.add(String.format("有 %d 次生成耗时超过 120 秒，建议检查供应商响应、素材体积和异步任务并发。", slowCount));
            }
            if (noAudioCount * 100.0 / total > 50) {
                result.add("超过一半生成记录未带音频，若目标是成片发布，请补齐 BGM/旁白链路以提升完整度。");
            }
            return result;
        } catch (Exception e) {
            log.warn("AI 反思生成异常: {}", e.getMessage());
            return List.of("AI 反思生成失败，请检查 sv_generation_log 聚合查询和质量评分数据。");
        }
    }

    private java.util.Optional<String> bestProviderReflection(Long ownerId, Timestamp start, Timestamp end) {
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
        if (bestModel == null) return java.util.Optional.empty();
        return java.util.Optional.of(String.format("模型 %s 近 7 天平均分最高（%.1f），建议优先用于同类项目。", bestModel, bestAvg));
    }

    private java.util.Optional<String> bestCameraReflection(Long ownerId, Timestamp start, Timestamp end) {
        String bestCamera = null;
        double bestAvg = 0;
        for (Object[] row : generationLogRepository.avgQualityByCameraType(ownerId, start, end)) {
            String camera = (String) row[0];
            BigDecimal avg = (BigDecimal) row[1];
            if (avg != null && avg.doubleValue() > bestAvg) {
                bestAvg = avg.doubleValue();
                bestCamera = camera;
            }
        }
        if (bestCamera == null) return java.util.Optional.empty();
        return java.util.Optional.of(String.format("运镜 %s 近 7 天质量分最高（%.1f），建议沉淀到分镜模板。", bestCamera, bestAvg));
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
