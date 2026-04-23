package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.script.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文案/话术高质量：基于 LlmClient + ai_model，优先 Claude Opus
 */
@Service
@Primary
public class LlmClientAiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(LlmClientAiServiceImpl.class);
    private static final String SCRIPT_SYSTEM = "你是专业的直播话术撰写专家，生成高质量、有感染力的直播话术。要求：开场吸引、卖点突出、促销有力、行动号召明确。";

    @Resource
    private LlmClient llmClient;
    @Resource
    private AiModelRepository aiModelRepository;
    @Autowired(required = false)
    private DeepSeekAiServiceImpl deepSeekFallback;

    @Override
    public String generateScript(String prompt) {
        AiModel model = resolveQualityModel();
        if (model != null) {
            LlmClient.LlmResponse resp = llmClient.chat(model, SCRIPT_SYSTEM, prompt);
            if (resp.success() && resp.content() != null && !resp.content().isBlank()) {
                return resp.content();
            }
            log.warn("LlmClient 话术生成失败，尝试 fallback: {}", resp.errorMsg());
        }
        if (deepSeekFallback != null) {
            return deepSeekFallback.generateScript(prompt);
        }
        log.error("无可用 AI 模型，请配置 ai_model 或 DeepSeek");
        return "[AI 生成失败：请先配置 Claude Opus 或 DeepSeek 模型]";
    }

    @Override
    public List<String> generateScriptBatch(List<String> prompts) {
        return prompts.stream().map(this::generateScript).collect(Collectors.toList());
    }

    @Override
    public String optimizeScript(String content, String style) {
        String prompt = String.format(
                "请优化以下直播话术，风格为 %s:\n\n%s\n\n要求：保持原意，增强 %s 风格，控制在 200-300 字。",
                style, content, style);
        return generateScript(prompt);
    }

    @Override
    public Double scoreScript(String content) {
        String prompt = String.format("请评分以下直播话术 (1-10 分)，只返回数字:\n\n%s", content);
        AiModel model = resolveQualityModel();
        if (model == null) return 8.5;
        LlmClient.LlmResponse resp = llmClient.chat(model, "你是一个话术评分专家，只输出 1-10 的数字。", prompt);
        if (!resp.success() || resp.content() == null) return 8.5;
        try {
            double score = Double.parseDouble(resp.content().trim().replaceAll("[^0-9.]", ""));
            return Math.min(Math.max(score, 1.0), 10.0);
        } catch (Exception e) {
            log.warn("评分解析失败，使用默认 8.5");
            return 8.5;
        }
    }

    /** 优先 Claude Opus */
    private AiModel resolveQualityModel() {
        List<AiModel> all = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (all == null || all.isEmpty()) return null;
        return all.stream()
                .filter(m -> m.getModelVersion() != null && m.getModelVersion().toLowerCase().contains("opus"))
                .findFirst()
                .orElse(all.get(0));
    }
}
