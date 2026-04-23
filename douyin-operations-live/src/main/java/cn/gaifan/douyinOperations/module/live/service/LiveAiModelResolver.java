package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 统一模型解析器 — 替换 LiveAiServiceImpl 中多种不同的模型解析模式
 * <p>
 * 解析优先级：
 * 1. 前端传入的 modelId（非 null 且 > 0）
 * 2. TaskModelConfig 中配置的任务模型（primaryModelId → fallbackModelId）
 * 3. 回退到第一个可用模型
 */
@Component
public class LiveAiModelResolver {

    private static final Logger log = LoggerFactory.getLogger(LiveAiModelResolver.class);

    /** 话术生成使用模型配置中的 copy_processing */
    private static final String TASK_CODE_COPY_PROCESSING = "copy_processing";

    @Resource
    private AiModelRepository aiModelRepository;

    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;

    /**
     * 根据任务类型解析可用模型
     *
     * @param explicitModelId 前端传入的模型 ID（可选）
     * @param taskType        任务类型标识（如 "copy_processing"、"live_generation" 等）
     * @return 解析出的 AiModel，若未找到则抛出异常
     */
    public AiModel resolveModel(Long explicitModelId, String taskType) {
        // 1. Explicit model ID
        if (explicitModelId != null && explicitModelId > 0) {
            Optional<AiModel> model = aiModelRepository.findById(explicitModelId);
            if (model.isPresent()) return model.get();
            log.warn("指定模型 ID {} 不存在，回退到任务配置", explicitModelId);
        }

        // 2. TaskModelConfig (status=1 means enabled)
        String code = taskType != null ? taskType : TASK_CODE_COPY_PROCESSING;
        Optional<AiTaskModelConfig> config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(code, 1, 0);
        if (config.isPresent()) {
            AiTaskModelConfig cfg = config.get();
            // Try primary → fallback → fallback2
            if (cfg.getPrimaryModelId() != null) {
                Optional<AiModel> model = aiModelRepository.findById(cfg.getPrimaryModelId());
                if (model.isPresent()) return model.get();
            }
            if (cfg.getFallbackModelId() != null) {
                Optional<AiModel> model = aiModelRepository.findById(cfg.getFallbackModelId());
                if (model.isPresent()) return model.get();
            }
            if (cfg.getFallback2ModelId() != null) {
                Optional<AiModel> model = aiModelRepository.findById(cfg.getFallback2ModelId());
                if (model.isPresent()) return model.get();
            }
        }

        // 3. Fallback: copy_processing config
        if (!TASK_CODE_COPY_PROCESSING.equals(code)) {
            Optional<AiTaskModelConfig> fallbackConfig = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(TASK_CODE_COPY_PROCESSING, 1, 0);
            if (fallbackConfig.isPresent() && fallbackConfig.get().getPrimaryModelId() != null) {
                Optional<AiModel> model = aiModelRepository.findById(fallbackConfig.get().getPrimaryModelId());
                if (model.isPresent()) return model.get();
            }
        }

        // 4. Last resort: first available model
        List<AiModel> allModels = aiModelRepository.findAll();
        if (!allModels.isEmpty()) {
            log.warn("未找到任务模型配置 (taskCode={}), 使用第一个可用模型", code);
            return allModels.get(0);
        }

        throw new cn.gaifan.douyinOperations.common.exception.BusinessException(
                cn.gaifan.douyinOperations.common.constant.ErrorCode.AI_QUOTA_EXCEEDED,
                "未找到可用的 AI 模型配置");
    }

    /**
     * 解析模型，使用默认任务类型 copy_processing
     */
    public AiModel resolveModel(Long explicitModelId) {
        return resolveModel(explicitModelId, null);
    }

    /**
     * 解析用于流式输出的模型 ID
     */
    public Long resolveModelIdForStream(Long explicitModelId) {
        return resolveModel(explicitModelId).getId();
    }
}
