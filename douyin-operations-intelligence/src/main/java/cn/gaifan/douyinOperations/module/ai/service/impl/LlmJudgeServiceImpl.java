package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.LlmJudgeService;
import cn.gaifan.douyinOperations.module.ai.util.EvolveModelOrderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 裁判服务实现：调用 LLM 对进化报告质量打分（0-100），启发式评分作为 fallback
 */
@Service
public class LlmJudgeServiceImpl implements LlmJudgeService {

    private static final Logger log = LoggerFactory.getLogger(LlmJudgeServiceImpl.class);

    private static final String JUDGE_SYSTEM = """
            你是一位专业的内容质量评审专家，专注于抖音护肤/彩妆领域的直播话术与知识内容。
            请对给定的进化报告进行客观、严格的质量评分（0-100分），评分标准如下：
            - 内容完整性（30分）：是否有完整的分析框架、关键章节齐全
            - 实用价值（30分）：是否包含可落地的建议、具体数据或案例
            - 深度与洞察（20分）：是否有超越表面的深度分析
            - 表达质量（10分）：结构清晰、表达准确
            - 行业相关性（10分）：内容是否契合护肤/彩妆直播场景
            
            只返回0-100的整数分值，不要任何解释或其他文本。
            """;

    private static final String HUASHU_JUDGE_SYSTEM = """
            你是抖音护肤/彩妆直播话术专家评审。请对话术进化报告评分（0-100分）：
            - 话术可用性（40分）：是否提供可直接使用的话术片段
            - 场景覆盖（25分）：是否覆盖开场/卖点/互动/促单/收尾等关键场景
            - 差异化（20分）：是否有区别于通用模板的创新表达
            - 合规性（15分）：是否规避明显的违规词/绝对化表述
            
            只返回0-100的整数分值。
            """;

    private static final String ZHISHI_JUDGE_SYSTEM = """
            你是护肤/彩妆知识库质量评审专家。请对知识进化报告评分（0-100分）：
            - 技术准确性（35分）：成分/功效知识是否准确
            - 实践指导性（30分）：是否有具体的操作指南或注意事项
            - 知识深度（20分）：是否覆盖进阶知识点
            - 引用与溯源（15分）：是否有数据或案例支撑
            
            只返回0-100的整数分值。
            """;

    private final AiModelRepository modelRepository;
    private final LlmClient llmClient;

    @Value("${app.ai.llm-judge.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.llm-judge.timeout-seconds:30}")
    private int timeoutSeconds;

    public LlmJudgeServiceImpl(AiModelRepository modelRepository, LlmClient llmClient) {
        this.modelRepository = modelRepository;
        this.llmClient = llmClient;
    }

    @Override
    public boolean isAvailable() {
        if (!enabled) return false;
        if (llmClient == null || modelRepository == null) return false;
        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0);
        return models != null && !models.isEmpty();
    }

    @Override
    public int judgeReportQuality(String reportType, String content, String topicTitle) {
        if (!isAvailable() || content == null || content.isBlank()) {
            return -1;
        }
        try {
            List<AiModel> models = EvolveModelOrderUtil.buildOrderedChain(
                    modelRepository.findByStatusAndDeleted(1, 0));
            if (models.isEmpty()) return -1;

            String system = selectSystemPrompt(reportType);
            String prompt = buildPrompt(content, topicTitle);

            LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, system, prompt);
            if (!resp.success() || resp.content() == null) return -1;

            String text = resp.content().trim().replaceAll("[^0-9]", "");
            if (text.isEmpty()) return -1;
            int score = Integer.parseInt(text);
            return Math.min(100, Math.max(0, score));
        } catch (Exception e) {
            log.warn("[LlmJudge] 质量评分失败，降级到启发式评分: {}", e.getMessage());
            return -1;
        }
    }

    @Override
    public List<Integer> judgeReportQualityBatch(String reportType, List<String> contents) {
        List<Integer> results = new ArrayList<>(contents.size());
        for (String content : contents) {
            results.add(judgeReportQuality(reportType, content, null));
        }
        return results;
    }

    private String selectSystemPrompt(String reportType) {
        if ("huashu".equals(reportType)) return HUASHU_JUDGE_SYSTEM;
        if ("zhishi".equals(reportType)) return ZHISHI_JUDGE_SYSTEM;
        return JUDGE_SYSTEM;
    }

    private String buildPrompt(String content, String topicTitle) {
        String truncated = content.length() > 2000 ? content.substring(0, 2000) + "..." : content;
        if (topicTitle != null && !topicTitle.isBlank()) {
            return "主题：" + topicTitle + "\n\n报告内容：\n" + truncated;
        }
        return "报告内容：\n" + truncated;
    }
}
