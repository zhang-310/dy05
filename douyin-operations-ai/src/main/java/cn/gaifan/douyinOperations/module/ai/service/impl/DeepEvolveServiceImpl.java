package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.repository.*;
import cn.gaifan.douyinOperations.module.ai.service.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 深度进化 Agent：针对待深化问题做专项研究，多轮迭代提升知识质量
 */
@Service
public class DeepEvolveServiceImpl implements DeepEvolveService {

    private static final Logger log = LoggerFactory.getLogger(DeepEvolveServiceImpl.class);
    private static final int QUALITY_THRESHOLD = 50;

    @Resource
    private AiEvolvePendingDeepenRepository pendingDeepenRepository;

    @Resource
    private AiEvolveTaskRepository taskRepository;

    @Resource
    private AiIndexQueueRepository indexQueueRepository;

    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private IndexQueueAmqpPublisher indexQueueAmqpPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int runDeepEvolve(Long kbId, int maxCount) {
        if (kbId == null) return 0;

        List<AiEvolvePendingDeepen> pending = pendingDeepenRepository
                .findByStatusOrderByPriorityLevelAscCreateTimeAsc("pending", PageRequest.of(0, maxCount));

        if (pending.isEmpty()) return 0;

        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0).stream()
                .filter(m -> "ollama".equalsIgnoreCase(m.getModelProvider()) || "deepseek".equalsIgnoreCase(m.getModelProvider()) || "580ai".equalsIgnoreCase(m.getModelProvider()))
                .limit(3)
                .toList();
        if (models.isEmpty()) {
            log.warn("无可用模型，跳过深度进化");
            return 0;
        }

        int indexed = 0;
        for (AiEvolvePendingDeepen pd : pending) {
            try {
                String report = generateDeepReport(pd, models);
                if (report == null || report.length() < 200) {
                    pd.setStatus("failed");
                    pendingDeepenRepository.save(pd);
                    continue;
                }

                int score = scoreReport(report);
                if (score >= QUALITY_THRESHOLD) {
                    AiIndexQueue q = new AiIndexQueue();
                    q.setSourceType("evolved");
                    q.setSourceId(pd.getReportId());
                    q.setTargetKbId(kbId);
                    q.setContent(report);
                    q.setPriority(0);
                    indexQueueRepository.save(q);
                    if (indexQueueAmqpPublisher != null) indexQueueAmqpPublisher.notifyIndexTask();
                    indexed++;
                }
                pd.setStatus("done");
                pendingDeepenRepository.save(pd);
            } catch (Exception e) {
                log.warn("深度进化失败 questionId={}: {}", pd.getId(), e.getMessage());
                pd.setStatus("failed");
                pendingDeepenRepository.save(pd);
            }
        }
        if (indexed > 0) log.info("深度进化完成: 处理 {} 个问题, 入库 {} 篇", pending.size(), indexed);
        return indexed;
    }

    private String generateDeepReport(AiEvolvePendingDeepen pd, List<AiModel> models) {
        String prompt = String.format("""
                请针对以下待深化问题，进行深度研究并输出完整报告。

                【待深化问题】
                %s

                【输出格式】
                ## 方法论提炼
                3-5 条可执行的方法论，每条含具体步骤或依据

                ## 待深化问题
                1-2 个仍需进一步研究的问题（如有）

                ## 可迭代建议
                后续可优化的方向

                请用中文输出，内容需有数据支撑或行业案例。""", pd.getQuestionText());

        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, "你是抖音运营领域深度研究专家，输出严谨、可落地的方法论。", prompt);
        return resp.success() && resp.content() != null ? resp.content() : null;
    }

    private int scoreReport(String report) {
        if (report == null) return 0;
        int score = 50;
        if (report.contains("## 方法论提炼") && report.contains("## 待深化问题")) score += 20;
        if (report.contains("## 可迭代建议")) score += 10;
        Matcher m = Pattern.compile("\\d+[.、]").matcher(report);
        int bullets = 0;
        while (m.find()) bullets++;
        if (bullets >= 5) score += 10;
        if (report.length() > 800) score += 10;
        return Math.min(100, score);
    }
}
