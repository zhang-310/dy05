package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * AI 模型解析工具：集中管理模型选择、fallback、copy_processing 任务配置等逻辑。
 * 从 LiveAiServiceImpl 提取，供 Generation / Analysis 子服务共用。
 */
@Component
public class LiveAiModelHelper {

    private static final Logger log = LoggerFactory.getLogger(LiveAiModelHelper.class);
    private static final String TASK_CODE_COPY_PROCESSING = "copy_processing";

    @Resource private AiModelRepository aiModelRepository;
    @Resource private AiTaskModelConfigRepository taskModelConfigRepository;

    /** modelId 非空时用指定模型，否则使用 copy_processing 任务配置 */
    public AiModel findAvailableModel(Long modelId) {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models == null || models.isEmpty()) return null;
        if (modelId != null && modelId > 0) {
            AiModel specified = models.stream().filter(m -> modelId.equals(m.getId())).findFirst().orElse(null);
            if (specified != null) {
                log.debug("话术使用指定模型: modelId={}, model={}", modelId, specified.getModelVersion());
                return specified;
            }
            log.warn("指定模型 modelId={} 未在启用列表中，将使用 copy_processing 配置", modelId);
        }
        List<AiModel> fromTask = resolveModelsFromCopyProcessing();
        if (!fromTask.isEmpty()) {
            AiModel first = fromTask.stream()
                    .filter(m -> m.getQuotaLimit() == null || m.getQuotaLimit() == 0
                            || (m.getQuotaUsed() != null && m.getQuotaUsed() < m.getQuotaLimit()))
                    .findFirst()
                    .orElse(fromTask.get(0));
            log.debug("话术使用 copy_processing 配置: model={}", first.getModelVersion());
            return first;
        }
        AiModel fallback = models.stream()
                .filter(m -> m.getQuotaLimit() == null || m.getQuotaLimit() == 0
                        || (m.getQuotaUsed() != null && m.getQuotaUsed() < m.getQuotaLimit()))
                .findFirst()
                .orElse(models.get(0));
        log.debug("话术 copy_processing 无配置，回退到默认模型: model={}", fallback != null ? fallback.getModelVersion() : "null");
        return fallback;
    }

    public AiModel findAvailableModel() {
        return findAvailableModel(null);
    }

    /** 解析要使用的模型列表：modelId 非空时用指定模型，否则使用 copy_processing 配置 */
    public List<AiModel> resolveModels(Long modelId) {
        List<AiModel> all = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (all == null || all.isEmpty()) return Collections.emptyList();
        if (modelId != null && modelId > 0) {
            return all.stream().filter(m -> modelId.equals(m.getId())).findFirst()
                    .map(List::of)
                    .orElse(resolveModelsFromCopyProcessing().isEmpty() ? resolveQualityModelFallback(all) : resolveModelsFromCopyProcessing());
        }
        List<AiModel> fromTask = resolveModelsFromCopyProcessing();
        return fromTask.isEmpty() ? resolveQualityModelFallback(all) : fromTask;
    }

    /** 解析流式调用用的 modelId */
    public Long resolveModelIdForStream(Long modelId) {
        List<AiModel> all = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (all == null || all.isEmpty()) return null;
        if (modelId != null && modelId > 0) {
            return all.stream().filter(m -> modelId.equals(m.getId())).findFirst()
                    .map(AiModel::getId)
                    .orElseGet(() -> {
                        List<AiModel> fromTask = resolveModelsFromCopyProcessing();
                        return fromTask.isEmpty() ? all.get(0).getId() : fromTask.get(0).getId();
                    });
        }
        List<AiModel> fromTask = resolveModelsFromCopyProcessing();
        if (!fromTask.isEmpty()) return fromTask.get(0).getId();
        AiModel opus = all.stream().filter(m -> m.getModelVersion() != null && m.getModelVersion().toLowerCase().contains("opus"))
                .findFirst().orElse(null);
        return (opus != null ? opus : all.get(0)).getId();
    }

    public void incrementQuotaUsed(Long modelId, long tokensUsed) {
        if (modelId != null && tokensUsed > 0) {
            aiModelRepository.incrementQuotaUsed(modelId, tokensUsed);
        }
    }

    private List<AiModel> resolveModelsFromCopyProcessing() {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(TASK_CODE_COPY_PROCESSING, 1, 0);
        if (config.isEmpty()) return Collections.emptyList();
        AiTaskModelConfig tc = config.get();
        List<AiModel> result = new ArrayList<>();
        for (Long id : Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
            if (id == null) continue;
            aiModelRepository.findById(id).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0).ifPresent(result::add);
        }
        return result;
    }

    private List<AiModel> resolveQualityModelFallback(List<AiModel> all) {
        AiModel opus = all.stream().filter(m -> m.getModelVersion() != null && m.getModelVersion().toLowerCase().contains("opus"))
                .findFirst().orElse(null);
        if (opus != null) return List.of(opus);
        return all.isEmpty() ? Collections.emptyList() : List.of(all.get(0));
    }
}
