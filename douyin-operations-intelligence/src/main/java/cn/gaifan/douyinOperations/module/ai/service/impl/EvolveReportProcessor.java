package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolvePendingDeepenRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiIndexQueueRepository;
import cn.gaifan.douyinOperations.module.ai.service.IndexQueueAmqpPublisher;
import cn.gaifan.douyinOperations.module.ai.service.LlmJudgeService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 进化报告处理器：负责报告质量评分、报告结构修正、章节提取、待深化问题提取、索引队列推送等。
 */
@Component
public class EvolveReportProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvolveReportProcessor.class);

    @Value("${app.ai.evolve.deepen-threshold:25}")
    private int deepenThreshold;

    @Resource
    private AiEvolveReportRepository reportRepository;

    @Resource
    private AiEvolvePendingDeepenRepository pendingDeepenRepository;

    @Resource
    private AiIndexQueueRepository indexQueueRepository;

    @Autowired(required = false)
    private IndexQueueAmqpPublisher indexQueueAmqpPublisher;

    @Autowired(required = false)
    private LlmJudgeService llmJudgeService;

    // ---- report structure fix ----

    String fixReportStructure(String raw) {
        if (raw == null) return "";
        if (!raw.contains("## 方法论提炼")) raw = "## 方法论提炼\n\n（待补充）\n\n" + raw;
        if (!raw.contains("## 待深化问题")) raw = raw + "\n\n## 待深化问题\n\n（待补充）";
        if (!raw.contains("## 可迭代建议")) raw = raw + "\n\n## 可迭代建议\n\n（待补充）";
        return raw;
    }

    String fixHuashuReportStructure(String content) {
        if (content == null || content.isBlank()) return content;
        return content.trim();
    }

    String fixZhishiReportStructure(String content) {
        if (content == null || content.isBlank()) return content;
        String s = content.trim();
        if (!s.contains("## 技术要点提炼")) s = "## 技术要点提炼\n\n（待补充）\n\n" + s;
        if (!s.contains("## 待深化问题")) s = s + "\n\n## 待深化问题\n\n（待补充）";
        if (!s.contains("## 实践检查清单")) s = s + "\n\n## 实践检查清单\n\n（待补充）";
        return s;
    }

    // ---- quality scoring ----

    /** 通用报告评分：LLM 优先，启发式兜底 */
    QualityScoreResult scoreReport(String content) {
        return scoreReport(content, null, "general");
    }

    QualityScoreResult scoreReport(String content, String topicTitle, String reportType) {
        if (llmJudgeService != null && llmJudgeService.isAvailable()) {
            try {
                int llmScore = llmJudgeService.judgeReportQuality(reportType, content, topicTitle);
                if (llmScore >= 0) {
                    log.debug("[EvolveReportProcessor] LLM 裁判评分: {} (type={})", llmScore, reportType);
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("source", "llm");
                    detail.put("total", llmScore);
                    try {
                        return new QualityScoreResult(llmScore, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(detail));
                    } catch (Exception e) {
                        return new QualityScoreResult(llmScore, "{\"source\":\"llm\"}");
                    }
                }
            } catch (Exception e) {
                log.warn("[EvolveReportProcessor] LLM 评分失败，降级到启发式: {}", e.getMessage());
            }
        }
        return scoreReportHeuristic(content);
    }

    /** 通用报告启发式评分（LLM 降级时使用） */
    private QualityScoreResult scoreReportHeuristic(String content) {
        int total = 0;
        Map<String, Object> detail = new LinkedHashMap<>();

        int methodologyScore = 0;
        if (content.contains("## 方法论提炼")) {
            methodologyScore = 25;
            long bulletCount = content.substring(content.indexOf("## 方法论提炼"))
                    .split("[-*•]").length - 1;
            if (bulletCount >= 3) methodologyScore += 5;
        }
        total += methodologyScore;
        detail.put("methodology", methodologyScore);

        int deepenScore = 0;
        if (content.contains("## 待深化问题")) {
            deepenScore = 20;
            long qCount = content.substring(content.indexOf("## 待深化问题"))
                    .split("[？?]").length - 1;
            if (qCount >= 2) deepenScore += 5;
        }
        total += deepenScore;
        detail.put("deepen", deepenScore);

        int iterateScore = content.contains("## 可迭代建议") ? 10 : 0;
        total += iterateScore;
        detail.put("iterate", iterateScore);

        int completenessBonus = (content.contains("## 方法论提炼") && content.contains("## 待深化问题")
                && content.contains("## 可迭代建议")) ? 10 : 0;
        total += completenessBonus;
        detail.put("completeness", completenessBonus);

        int expertBonus = 0;
        if (content.contains("[防踩坑]") || content.contains("失败")) expertBonus += 5;
        if (content.contains("步骤") || content.contains("1.") || content.contains("2.") || content.contains("SOP")) expertBonus += 5;
        if (content.contains("%") || content.contains("转化") || content.contains("均值")) expertBonus += 5;
        total += expertBonus;
        detail.put("expert", expertBonus);

        detail.put("total", total);
        try {
            return new QualityScoreResult(total, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(detail));
        } catch (Exception e) {
            return new QualityScoreResult(total, "{}");
        }
    }

    /** 知识库报告评分：LLM 优先，启发式兜底 */
    QualityScoreResult scoreZhishiReport(String content) {
        return scoreZhishiReport(content, null);
    }

    QualityScoreResult scoreZhishiReport(String content, String topicTitle) {
        if (llmJudgeService != null && llmJudgeService.isAvailable()) {
            try {
                int llmScore = llmJudgeService.judgeReportQuality("zhishi", content, topicTitle);
                if (llmScore >= 0) {
                    try {
                        return new QualityScoreResult(llmScore, new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(Map.of("source", "llm", "total", llmScore)));
                    } catch (Exception e) {
                        return new QualityScoreResult(llmScore, "{\"source\":\"llm\"}");
                    }
                }
            } catch (Exception e) {
                log.warn("[EvolveReportProcessor] LLM 知识报告评分失败: {}", e.getMessage());
            }
        }
        return scoreZhishiReportHeuristic(content);
    }

    private QualityScoreResult scoreZhishiReportHeuristic(String content) {
        int total = 0;
        Map<String, Object> detail = new LinkedHashMap<>();
        String techSection = content.contains("## 技术要点提炼")
                ? content.substring(content.indexOf("## 技术要点提炼")).split("##")[0] : "";
        int techScore = 0;
        if (!techSection.isBlank()) {
            techScore = 25;
            long bulletCount = techSection.split("[-*•]").length - 1;
            if (bulletCount >= 3) techScore += 10;
            if (techSection.contains("[防踩坑]") || techSection.contains("踩坑")) techScore += 5;
        }
        total += techScore;
        detail.put("tech", techScore);
        int checklistScore = content.contains("## 实践检查清单") ? 15 : 0;
        total += checklistScore;
        detail.put("checklist", checklistScore);
        int deepenScore = content.contains("## 待深化问题") ? 20 : 0;
        total += deepenScore;
        detail.put("deepen", deepenScore);
        total = Math.min(total, 100);
        detail.put("total", total);
        try {
            return new QualityScoreResult(total, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(detail));
        } catch (Exception e) {
            return new QualityScoreResult(total, "{}");
        }
    }

    /** 话术报告评分：LLM 优先，启发式兜底 */
    QualityScoreResult scoreHuashuReport(String content) {
        return scoreHuashuReport(content, null);
    }

    QualityScoreResult scoreHuashuReport(String content, String topicTitle) {
        if (llmJudgeService != null && llmJudgeService.isAvailable()) {
            try {
                int llmScore = llmJudgeService.judgeReportQuality("huashu", content, topicTitle);
                if (llmScore >= 0) {
                    try {
                        return new QualityScoreResult(llmScore, new com.fasterxml.jackson.databind.ObjectMapper()
                                .writeValueAsString(Map.of("source", "llm", "total", llmScore)));
                    } catch (Exception e) {
                        return new QualityScoreResult(llmScore, "{\"source\":\"llm\"}");
                    }
                }
            } catch (Exception e) {
                log.warn("[EvolveReportProcessor] LLM 话术报告评分失败: {}", e.getMessage());
            }
        }
        return scoreHuashuReportHeuristic(content);
    }

    private QualityScoreResult scoreHuashuReportHeuristic(String content) {
        int total = 0;
        Map<String, Object> detail = new LinkedHashMap<>();
        if (content.contains("话术") || content.contains("## 话术片段")) {
            int scriptScore = 40;
            long bulletCount = content.split("[-*•]").length;
            if (bulletCount >= 5) scriptScore += 20;
            else if (bulletCount >= 3) scriptScore += 10;
            total += scriptScore;
        }
        if (content.contains("开场") || content.contains("卖点") || content.contains("互动") || content.contains("收尾") || content.contains("秒杀")) {
            total += 15;
        }
        if (content.length() >= 500) total += 10;
        if (content.length() >= 800) total += 5;
        total = Math.min(total, 100);
        detail.put("script", total);
        detail.put("total", total);
        try {
            return new QualityScoreResult(total, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(detail));
        } catch (Exception e) {
            return new QualityScoreResult(total, "{}");
        }
    }

    // ---- report saving ----

    AiEvolveReport saveReport(AiEvolveTask task, String fullContent, QualityScoreResult scoreResult) {
        AiEvolveReport report = new AiEvolveReport();
        report.setTaskId(task.getId());
        report.setReportTitle("进化报告_" + task.getTaskNo());
        report.setFullContent(fullContent);

        boolean isZhishiFormat = fullContent.contains("## 技术要点提炼");
        String methodologySection = isZhishiFormat
                ? extractSection(fullContent, "## 技术要点提炼", "## 待深化问题")
                : extractSection(fullContent, "## 方法论提炼", "## 待深化问题");
        String deepenSection = extractSection(fullContent, "## 待深化问题", isZhishiFormat ? "## 实践检查清单" : "## 可迭代建议");
        String iterateSection = isZhishiFormat
                ? extractSection(fullContent, "## 实践检查清单", null)
                : extractSection(fullContent, "## 可迭代建议", null);

        report.setMethodologySection(methodologySection);
        report.setDeepenSection(deepenSection);
        report.setIterateSection(iterateSection);
        report.setMethodologyCount(countBullets(methodologySection));
        report.setDeepenCount(countQuestions(deepenSection));
        report.setHasFailureCase((fullContent.contains("[防踩坑]") || fullContent.contains("失败案例")) ? 1 : 0);
        report.setHasSop((fullContent.contains("步骤") || fullContent.contains("SOP")) ? 1 : 0);
        report.setHasBenchmark((fullContent.contains("%") || fullContent.contains("转化率")) ? 1 : 0);

        return reportRepository.save(report);
    }

    // ---- section extraction helpers ----

    String extractSection(String full, String startMarker, String endMarker) {
        int start = full.indexOf(startMarker);
        if (start < 0) return "";
        start = full.indexOf("\n", start) + 1;
        int end = endMarker != null ? full.indexOf(endMarker, start) : full.length();
        if (end < 0) end = full.length();
        return full.substring(start, end).trim();
    }

    int countBullets(String text) {
        if (text == null) return 0;
        return (int) Pattern.compile("[-*•]").matcher(text).results().count();
    }

    int countQuestions(String text) {
        if (text == null) return 0;
        return (int) Pattern.compile("[？?]").matcher(text).results().count();
    }

    // ---- deepen questions extraction ----

    void extractDeepenQuestions(AiEvolveReport report, AiEvolveTask task) {
        String deepenSection = report.getDeepenSection();
        if (deepenSection == null || deepenSection.isBlank()) return;

        Pattern p = Pattern.compile("[^\\n]+[？?][^\\n]*");
        Matcher m = p.matcher(deepenSection);
        while (m.find()) {
            String q = m.group().trim();
            if (q.length() > 10) {
                AiEvolvePendingDeepen pd = new AiEvolvePendingDeepen();
                pd.setReportId(report.getId());
                pd.setTaskId(task.getId());
                pd.setKbId(task.getKbId());
                pd.setQuestionText(q);
                pd.setPriorityLevel(task.getScoreTotal() != null && task.getScoreTotal() < deepenThreshold ? 0 : 1);
                pendingDeepenRepository.save(pd);
            }
        }
    }

    // ---- index queue ----

    void pushToIndexQueue(AiEvolveReport report, Long targetKbId, boolean isHuashu) {
        AiIndexQueue q = new AiIndexQueue();
        q.setSourceType(isHuashu ? "evolved_script" : "evolved");
        q.setSourceId(report.getId());
        q.setTargetKbId(targetKbId);
        q.setContent(report.getFullContent());
        q.setPriority(1);
        indexQueueRepository.save(q);
        if (indexQueueAmqpPublisher != null) {
            indexQueueAmqpPublisher.notifyIndexTask();
        }
    }

    // ---- inner record ----

    record QualityScoreResult(int total, String detailJson) {}
}
