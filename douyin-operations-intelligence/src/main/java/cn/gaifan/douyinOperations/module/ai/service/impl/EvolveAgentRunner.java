package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.AiPromptConfigService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 进化引擎扩展 Agent 运行器：竞品分析、反馈驱动、多模态索引等独立 Agent 的执行逻辑。
 */
@Component
public class EvolveAgentRunner {

    private static final Logger log = LoggerFactory.getLogger(EvolveAgentRunner.class);

    @Value("${app.ai.evolution.feedback-low-score-threshold:40}")
    private int feedbackLowScoreThreshold;

    @Resource
    private AiPromptConfigService aiPromptConfigService;

    @Resource
    private LlmClient llmClient;

    @Resource
    private cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository taskRepository;

    String runCompetitorKnowledgeAgent(Long userId, String competitorInfo, List<AiModel> models) {
        if (models == null || models.isEmpty()) {
            log.warn("[竞品Agent] 无可用模型，跳过");
            return "无可用模型";
        }

        String systemPrompt = aiPromptConfigService.getPrompt("ai.prompt.competitor.system",
                "你是一位资深抖音竞品分析师，擅长从竞品账号的热门内容中提取可学习的知识要点和运营策略。");
        String prompt = "请分析以下竞品信息，提取可学习的知识要点：\n\n" + competitorInfo +
                "\n\n请输出：\n## 竞品优势分析\n- 列出竞品的核心优势和差异化策略\n" +
                "\n## 可借鉴知识点\n- 至少 3 条可直接应用的运营策略或内容技巧\n" +
                "\n## 行动建议\n- 针对当前账号的具体改进建议";

        LlmClient.LlmResponse response = llmClient.chatWithFallback(models, systemPrompt, prompt);
        if (!response.success()) {
            log.error("[竞品Agent] LLM 分析失败: {}", response.errorMsg());
            return "分析失败: " + response.errorMsg();
        }

        log.info("[竞品Agent] userId={} 竞品分析完成, 内容长度={}", userId, response.content().length());
        return response.content();
    }

    String runFeedbackDrivenAgent(Long userId, List<AiModel> models) {
        if (models == null || models.isEmpty()) {
            log.warn("[反馈Agent] 无可用模型，跳过");
            return "无可用模型";
        }

        String lowScoreContext = "";
        try {
            java.sql.Timestamp since = java.sql.Timestamp.valueOf(
                    LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay());
            List<cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask> recentTasks = taskRepository.findScoredTasksSince(since);
            StringBuilder sb = new StringBuilder();
            for (var t : recentTasks) {
                if (t.getScoreTotal() != null && t.getScoreTotal() < feedbackLowScoreThreshold) {
                    sb.append("- 任务 ").append(t.getTaskNo())
                            .append("，得分 ").append(t.getScoreTotal())
                            .append("，角度 ").append(t.getEvolveAngle())
                            .append("，主题 ").append(t.getTopicTexts())
                            .append("\n");
                }
                if (sb.length() > 3000) break;
            }
            lowScoreContext = sb.toString();
        } catch (Exception e) {
            log.warn("[反馈Agent] 获取低评分数据失败: {}", e.getMessage());
        }

        if (lowScoreContext.isBlank()) {
            lowScoreContext = "（暂无低评分话术数据）";
        }

        String systemPrompt = aiPromptConfigService.getPrompt("ai.prompt.feedback.system",
                "你是一位直播话术质量诊断专家，擅长从低效话术中分析失败原因并提取知识缺口。");
        String prompt = "以下是效果评分低于 " + feedbackLowScoreThreshold + " 分的话术汇总：\n\n" + lowScoreContext +
                "\n\n请分析：\n## 失败原因分析\n- 逐条分析低分话术的核心问题\n" +
                "\n## 知识缺口\n- 提取至少 3 个需要补充的知识点或技能\n" +
                "\n## 改进方案\n- 针对每个知识缺口给出具体的改进建议";

        LlmClient.LlmResponse response = llmClient.chatWithFallback(models, systemPrompt, prompt);
        if (!response.success()) {
            log.error("[反馈Agent] LLM 分析失败: {}", response.errorMsg());
            return "分析失败: " + response.errorMsg();
        }

        log.info("[反馈Agent] userId={} 反馈分析完成, 内容长度={}", userId, response.content().length());
        return response.content();
    }

    String runMultiModalIndexAgent(Long userId, String mediaUrl, String mediaType, List<AiModel> models) {
        if (models == null || models.isEmpty()) {
            log.warn("[多模态Agent] 无可用模型，跳过");
            return "无可用模型";
        }

        String systemPrompt = aiPromptConfigService.getPrompt("ai.prompt.multimodal.system",
                "你是一位视觉内容分析专家，擅长为图片和视频内容生成精准的文字描述，用于知识索引和检索。");
        String prompt;
        if ("image".equalsIgnoreCase(mediaType)) {
            prompt = "请为以下图片生成详细的文字描述，包括：主题、场景、元素、风格、适用场景。\n用于知识库索引，需包含关键词便于检索。";
        } else {
            prompt = "请为以下视频内容生成详细的文字描述，包括：主题、剧情/内容摘要、风格、目标受众、适用场景。\n用于知识库索引，需包含关键词便于检索。\n\n媒体地址: " + mediaUrl;
        }

        LlmClient.LlmResponse response;
        if ("image".equalsIgnoreCase(mediaType) && mediaUrl != null) {
            response = llmClient.chatWithImageFallback(models, systemPrompt, prompt, List.of(mediaUrl));
        } else {
            response = llmClient.chatWithFallback(models, systemPrompt, prompt);
        }

        if (!response.success()) {
            log.error("[多模态Agent] LLM 描述生成失败: {}", response.errorMsg());
            return "描述生成失败: " + response.errorMsg();
        }

        log.info("[多模态Agent] userId={} mediaType={} 描述生成完成, 内容长度={}",
                userId, mediaType, response.content().length());
        return response.content();
    }
}
