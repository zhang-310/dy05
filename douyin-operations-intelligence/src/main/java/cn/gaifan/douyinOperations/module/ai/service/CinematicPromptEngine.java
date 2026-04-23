package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.domain.CameraType;
import cn.gaifan.douyinOperations.module.ai.domain.QualityLevel;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 电影级 Prompt 引擎
 * 根据分镜上下文生成针对性的 AI 视频生成 Prompt
 *
 * 设计原则 (附录 A.4):
 * 1. SD/HD: 纯模板+规则，快速生成 (不依赖外部服务)
 * 2. FHD/4K: 优先 LLM 增强，失败时降级为规则模板
 * 3. 输出 prompt 适配所有主流 AI 视频模型
 * 4. prompt 长度控制在 200 字符以内 (Kling 限制较严格)
 */
@Service
public class CinematicPromptEngine {

    private static final Logger log = LoggerFactory.getLogger(CinematicPromptEngine.class);

    @Autowired(required = false)
    private LlmClient llmClient;

    @Resource
    private AiModelRepository modelRepository;

    /**
     * 生成电影级 Prompt
     * FHD/4K 级别优先使用 LLM 增强，SD/HD 使用规则模板
     */
    public String generatePrompt(String sceneDescription, CameraType cameraType,
                                  String mood, String action, QualityLevel quality) {
        // FHD/4K 级别: 尝试 LLM 增强 (附录 A.1)
        if ((quality == QualityLevel.PREMIUM_FHD || quality == QualityLevel.CINEMA_4K)
                && llmClient != null) {
            try {
                String llmPrompt = generateWithLlm(sceneDescription, cameraType, mood, action);
                if (StringUtils.hasText(llmPrompt)) {
                    return llmPrompt;
                }
                log.warn("LLM 增强返回空结果，降级到规则模板模式");
            } catch (Exception e) {
                log.warn("LLM 增强不可用，降级到规则模板: {}", e.getMessage());
            }
        }

        // 规则模板模式 (fallback)
        return generateWithTemplate(sceneDescription, cameraType, mood, action, quality);
    }

    private String generateWithLlm(String sceneDescription, CameraType cameraType,
                                     String mood, String action) {
        String systemPrompt = """
            You are a professional cinematographer creating video generation prompts.
            Convert the Chinese scene description into a concise English video prompt (max 180 chars).
            Include: camera movement, lighting, mood, and key action.
            Output ONLY the prompt text, nothing else.
            """;

        String userInput = String.format(
            "场景: %s\n运镜: %s (%s)\n情绪: %s\n动作: %s",
            sceneDescription, cameraType.getZhName(), cameraType.getPromptFragment(),
            mood != null ? mood : "自然", action != null ? action : "无"
        );

        List<AiModel> models = modelRepository != null
                ? modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList()
                : new ArrayList<>();
        if (models.isEmpty()) return null;

        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userInput);
        if (resp == null || !resp.success() || !StringUtils.hasText(resp.content())) {
            return null;
        }
        String result = resp.content().trim();
        if (result.length() > 200) {
            result = result.substring(0, 200);
        }
        return result;
    }

    private String generateWithTemplate(String sceneDescription, CameraType cameraType,
                                          String mood, String action, QualityLevel quality) {
        StringBuilder prompt = new StringBuilder();

        // 1. 运镜指令
        prompt.append(cameraType.getPromptFragment());

        // 2. 场景描述
        if (StringUtils.hasText(sceneDescription)) {
            prompt.append(", ");
            String cleanDesc = sceneDescription.trim();
            if (cleanDesc.length() > 80) {
                cleanDesc = cleanDesc.substring(0, 80);
            }
            prompt.append(cleanDesc);
        }

        // 3. 动作描述
        if (StringUtils.hasText(action)) {
            prompt.append(", ");
            String cleanAction = action.trim();
            if (cleanAction.length() > 40) {
                cleanAction = cleanAction.substring(0, 40);
            }
            prompt.append(cleanAction);
        }

        // 4. 氛围/情绪
        if (StringUtils.hasText(mood)) {
            prompt.append(", ");
            prompt.append(mapMoodToPrompt(mood));
        }

        // 5. 质量后缀
        prompt.append(", ");
        prompt.append(getQualitySuffix(quality));

        return prompt.toString();
    }

    /**
     * 生成负向 prompt (仅用于支持 negative_prompt 的模型如 Runway/Pika)
     */
    public String generateNegativePrompt(QualityLevel quality) {
        StringBuilder neg = new StringBuilder();
        neg.append("blurry, low quality, distorted, deformed");
        if (quality == QualityLevel.PREMIUM_FHD || quality == QualityLevel.CINEMA_4K) {
            neg.append(", noise, artifacts, flickering, jitter, watermark");
        }
        return neg.toString();
    }

    private String mapMoodToPrompt(String mood) {
        if (mood == null) return "natural lighting";
        return switch (mood.toLowerCase()) {
            case "紧张", "tension", "suspense" -> "dramatic lighting, high contrast, tense atmosphere";
            case "温馨", "warm", "cozy" -> "warm golden light, soft tones, intimate atmosphere";
            case "悲伤", "sad", "melancholy" -> "cold blue tones, dim lighting, somber mood";
            case "欢快", "happy", "joyful" -> "bright vibrant colors, natural sunlight, cheerful energy";
            case "神秘", "mysterious", "dark" -> "dark shadows, silhouette lighting, mysterious ambiance";
            case "史诗", "epic", "grand" -> "epic wide angle, golden hour, majestic atmosphere";
            case "浪漫", "romantic" -> "soft bokeh, warm sunset light, romantic mood";
            case "恐怖", "horror", "scary" -> "dark shadows, unsettling atmosphere, eerie lighting";
            default -> "cinematic lighting, natural atmosphere";
        };
    }

    private String getQualitySuffix(QualityLevel quality) {
        return switch (quality) {
            case FAST_SD -> "smooth motion";
            case STANDARD_HD -> "cinematic quality, smooth motion";
            case PREMIUM_FHD -> "cinematic film quality, professional color grading, smooth natural motion";
            case CINEMA_4K -> "masterpiece cinematic quality, film grain, professional cinematography, ultra detailed";
        };
    }
}
