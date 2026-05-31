package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KnowledgeQualityScoreRepository;
import cn.gaifan.douyinOperations.module.ai.service.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 进化报告服务实现：周报、月报、看板数据聚合
 */
@Service
public class EvolutionReportServiceImpl implements EvolutionReportService {

    @Value("${app.ai.evolution.recent-tasks-default-limit:10}")
    private int recentTasksDefaultLimit;

    @Value("${app.ai.evolution.cold-doc-days:90}")
    private int coldDocDays;

    @Resource
    private EvolveEngineService evolveEngineService;
    @Resource
    private EvolveRoiService evolveRoiService;
    @Resource
    private AiEvolveTaskRepository taskRepository;
    @Resource
    private AiKbDocumentRepository documentRepository;
    @Autowired(required = false)
    private KnowledgeQualityScoreRepository qualityScoreRepository;
    @Autowired(required = false)
    private AiEvolveTopicRepository topicRepository;
    @Autowired(required = false)
    private ContentEffectivenessService contentEffectivenessService;

    @Override
    public Map<String, Object> getDashboardReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        // ROI 指标
        report.put("roiMetrics", evolveRoiService.getRoiMetrics(recentTasksDefaultLimit));
        report.put("knowledgeRefRate", evolveRoiService.getKnowledgeRefRate());
        report.put("shouldRun", evolveRoiService.shouldRun());

        // 周统计（近 7 天）
        Timestamp weekAgo = Timestamp.from(LocalDateTime.now().minusDays(7).atZone(ZoneId.systemDefault()).toInstant());
        List<AiEvolveTask> weekTasks = taskRepository.findScoredTasksSince(weekAgo);
        double weekAvgScore = weekTasks.stream()
                .filter(t -> t.getScoreTotal() != null)
                .mapToInt(AiEvolveTask::getScoreTotal)
                .average()
                .orElse(0);
        int weekIndexed = (int) weekTasks.stream()
                .filter(t -> t.getScoreTotal() != null && t.getScoreTotal() >= 50)
                .count();
        report.put("weekly", Map.of(
                "taskCount", weekTasks.size(),
                "avgScore", Math.round(weekAvgScore * 100) / 100.0,
                "indexedCount", weekIndexed
        ));

        // 月统计（近 30 天）
        Timestamp monthAgo = Timestamp.from(LocalDateTime.now().minusDays(30).atZone(ZoneId.systemDefault()).toInstant());
        List<AiEvolveTask> monthTasks = taskRepository.findScoredTasksSince(monthAgo);
        double monthAvgScore = monthTasks.stream()
                .filter(t -> t.getScoreTotal() != null)
                .mapToInt(AiEvolveTask::getScoreTotal)
                .average()
                .orElse(0);
        int monthIndexed = (int) monthTasks.stream()
                .filter(t -> t.getScoreTotal() != null && t.getScoreTotal() >= 50)
                .count();
        report.put("monthly", Map.of(
                "taskCount", monthTasks.size(),
                "avgScore", Math.round(monthAvgScore * 100) / 100.0,
                "indexedCount", monthIndexed
        ));

        // 评分趋势、主题分布
        report.put("scoreTrend", evolveEngineService.getScoreTrend(7));
        report.put("topicDistribution", evolveEngineService.getTopicDistribution());
        report.put("recentTasks", evolveEngineService.listRecentTasks(recentTasksDefaultLimit));

        // 各来源效果
        if (contentEffectivenessService != null) {
            Map<String, Object> effectiveness = new LinkedHashMap<>();
            for (String st : List.of("evolved", "viral_analysis", "live_review", "manual", "live_script")) {
                effectiveness.put(st, contentEffectivenessService.getEffectivenessBySourceType(st));
            }
            report.put("contentEffectiveness", effectiveness);
        }

        // 冷门文档数量（配置天数未检索/引用）
        long coldCount = documentRepository.countColdDocs(
                Timestamp.from(LocalDateTime.now().minusDays(coldDocDays).atZone(ZoneId.systemDefault()).toInstant()));
        report.put("coldDocCount", coldCount);

        // Phase 1 任务 1.3：知识进化仪表盘真实数据
        if (qualityScoreRepository != null) {
            BigDecimal avg = qualityScoreRepository.calculateGlobalAverageQualityScore();
            report.put("overallHealthScore", avg != null ? avg.doubleValue() : 0);
            LocalDate eightWeeksAgo = LocalDate.now().minusWeeks(8);
            try {
                List<Object[]> weekly = qualityScoreRepository.findWeeklyAvgScoresSince(Date.valueOf(eightWeeksAgo));
                List<Map<String, Object>> qualityTrend = new ArrayList<>();
                for (Object[] row : weekly) {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", row[0] != null ? row[0].toString() : "");
                    point.put("score", row[1] != null ? ((Number) row[1]).doubleValue() : 0);
                    qualityTrend.add(point);
                }
                report.put("qualityTrend", qualityTrend);
            } catch (Exception e) {
                report.put("qualityTrend", List.<Map<String, Object>>of());
            }
        } else {
            report.put("overallHealthScore", 0);
            report.put("qualityTrend", List.<Map<String, Object>>of());
        }
        if (topicRepository != null) {
            long topicCount = topicRepository.countByStatusAndDeleted(1, 0);
            long docCount = documentRepository.count();
            double coverage = topicCount > 0 ? Math.min(100, docCount * 100.0 / topicCount) : 0;
            report.put("knowledgeCoverage", coverage);
        } else {
            report.put("knowledgeCoverage", 0);
        }
        Timestamp now = Timestamp.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        Timestamp weekAgoTs = Timestamp.from(LocalDateTime.now().minusDays(7).atZone(ZoneId.systemDefault()).toInstant());
        Timestamp twoWeeksAgoTs = Timestamp.from(LocalDateTime.now().minusDays(14).atZone(ZoneId.systemDefault()).toInstant());
        long thisWeekCompleted = taskRepository.countCompletedBetween(weekAgoTs, now);
        long lastWeekCompleted = taskRepository.countCompletedBetween(twoWeeksAgoTs, weekAgoTs);
        double velocityGrowth = lastWeekCompleted > 0 ? (thisWeekCompleted - lastWeekCompleted) * 100.0 / lastWeekCompleted : (thisWeekCompleted > 0 ? 100 : 0);
        report.put("evolutionVelocity", velocityGrowth);

        return report;
    }
}
