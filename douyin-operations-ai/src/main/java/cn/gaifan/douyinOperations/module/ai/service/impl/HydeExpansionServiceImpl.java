package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.HydeExpansionService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HyDE：任务模型链优先 {@code kb_hyde}，其次 {@code query_rewrite}，再退化为任意可用模型。
 */
@Service
public class HydeExpansionServiceImpl implements HydeExpansionService {

    private static final Logger log = LoggerFactory.getLogger(HydeExpansionServiceImpl.class);

    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @Resource
    private LlmClient llmClient;

    @Value("${app.ai.search.hyde.enabled:false}")
    private boolean enabled;

    @Value("${app.ai.search.hyde.min-query-length:6}")
    private int minQueryLength;

    @Value("${app.ai.search.hyde.max-query-chars:800}")
    private int maxQueryChars;

    @Value("${app.ai.search.hyde.max-output-chars:480}")
    private int maxOutputChars;

    private static final String SYSTEM = "你是知识库检索助手。根据用户检索意图，写出一段「知识库文档里可能出现的」简短正文（2～4 句），语气客观、不要问答句式、不要编号列表、不要「根据用户」等元话语。只输出正文。";

    @Override
    public Optional<String> expandHypotheticalPassage(String queryText) {
        if (!enabled || queryText == null || queryText.isBlank()) {
            return Optional.empty();
        }
        String trimmed = queryText.trim();
        if (trimmed.length() < minQueryLength) {
            return Optional.empty();
        }
        String forLlm = trimmed.length() > maxQueryChars ? trimmed.substring(0, maxQueryChars) : trimmed;
        String prompt = "检索意图：\n" + forLlm + "\n\n请直接输出假设文档正文。";
        List<AiModel> models = resolveHydeModels();
        if (models.isEmpty()) {
            log.debug("HyDE 跳过：无可用模型");
            return Optional.empty();
        }
        try {
            LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, SYSTEM, prompt);
            if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
                return Optional.empty();
            }
            String body = resp.content().trim().replaceAll("\\s+", " ");
            if (body.length() > maxOutputChars) {
                body = body.substring(0, maxOutputChars);
            }
            if (body.length() < 12) {
                return Optional.empty();
            }
            return Optional.of(body);
        } catch (Exception e) {
            log.debug("HyDE 生成失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<AiModel> resolveHydeModels() {
        List<AiModel> fromTask = modelsFromTaskCode("kb_hyde");
        if (!fromTask.isEmpty()) return fromTask;
        fromTask = modelsFromTaskCode("query_rewrite");
        if (!fromTask.isEmpty()) return fromTask;
        return modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
    }

    private List<AiModel> modelsFromTaskCode(String taskCode) {
        Optional<AiTaskModelConfig> cfg = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(taskCode, 1, 0);
        if (cfg.isEmpty()) return List.of();
        AiTaskModelConfig tc = cfg.get();
        List<AiModel> result = new ArrayList<>();
        for (Long modelId : java.util.Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
            if (modelId == null) continue;
            modelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                    .ifPresent(result::add);
        }
        return result;
    }
}
