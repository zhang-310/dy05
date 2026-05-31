package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiModelBenchmark;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelBenchmarkRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.ModelBenchmarkService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ModelBenchmarkServiceImpl implements ModelBenchmarkService {

    @Resource
    private AiModelBenchmarkRepository benchmarkRepository;

    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private AiCallLogRepository callLogRepository;

    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBenchmark(Long modelId, String taskCode, long latencyMs, int tokensUsed, boolean success) {
        if (modelId == null || !StringUtils.hasText(taskCode)) {
            return;
        }
        AiModelBenchmark benchmark = new AiModelBenchmark();
        benchmark.setModelId(modelId);
        benchmark.setTaskCode(taskCode.trim());
        benchmark.setLatencyMs(Math.max(latencyMs, 0L));
        benchmark.setTokensUsed(Math.max(tokensUsed, 0));
        benchmark.setSuccess(success);
        benchmarkRepository.save(benchmark);
    }

    @Override
    public void recordFromCallLog(AiCallLog log) {
        if (log == null) {
            return;
        }
        Optional<Long> modelId = resolveModelId(log.getModelCode(), log.getCallType(), log.getTemplateCode());
        if (modelId.isEmpty()) {
            return;
        }
        String taskCode = resolveTaskCode(log.getCallType(), log.getTemplateCode());
        if (!StringUtils.hasText(taskCode)) {
            return;
        }
        recordBenchmark(
                modelId.get(),
                taskCode,
                log.getDurationMs() != null ? log.getDurationMs() : 0L,
                resolveTokens(log),
                log.getStatus() != null && log.getStatus() == 1);
    }

    @Override
    public List<Map<String, Object>> getModelComparison(String taskCode) {
        String normalizedTaskCode = StringUtils.hasText(taskCode) ? taskCode.trim() : null;
        List<Object[]> rows = StringUtils.hasText(normalizedTaskCode)
                ? benchmarkRepository.aggregateByTaskCode(normalizedTaskCode)
                : benchmarkRepository.aggregateAllByModelAndTask();
        if (rows == null || rows.isEmpty()) {
            return getModelComparisonFromCallLog(normalizedTaskCode);
        }
        return mapAggregateRows(rows);
    }

    private List<Map<String, Object>> getModelComparisonFromCallLog(String taskCode) {
        List<Object[]> rows = callLogRepository.aggregateModelBenchmarkFromCallLog(taskCode);
        Map<String, BenchmarkAccumulator> merged = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 7) {
                continue;
            }
            String callType = row[0] != null ? String.valueOf(row[0]) : "";
            String templateCode = row[1] != null ? String.valueOf(row[1]) : "";
            String modelCode = row[2] != null ? String.valueOf(row[2]) : "";
            Optional<Long> modelId = resolveModelId(modelCode, callType, templateCode);
            if (modelId.isEmpty()) {
                continue;
            }
            String tc = resolveTaskCode(callType, templateCode);
            if (!StringUtils.hasText(tc)) {
                continue;
            }
            long totalCalls = row[6] != null ? ((Number) row[6]).longValue() : 0L;
            if (totalCalls <= 0L) {
                continue;
            }
            String key = modelId.get() + "::" + tc;
            BenchmarkAccumulator accumulator = merged.computeIfAbsent(key, ignored ->
                    new BenchmarkAccumulator(modelId.get(), resolveModelName(modelId.get()), tc));
            accumulator.add(
                    row[3] != null ? ((Number) row[3]).doubleValue() : 0.0,
                    row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,
                    row[5] != null ? ((Number) row[5]).doubleValue() : 0.0,
                    totalCalls);
        }
        return merged.values().stream()
                .map(BenchmarkAccumulator::toMap)
                .toList();
    }

    private List<Map<String, Object>> mapAggregateRows(List<Object[]> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 6) {
                continue;
            }
            Long modelId = row[0] != null ? ((Number) row[0]).longValue() : null;
            if (modelId == null) {
                continue;
            }
            String tc = row[1] != null ? String.valueOf(row[1]) : "";
            double avgLatencyMs = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            double successRate = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            double avgTokens = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
            long totalCalls = row[5] != null ? ((Number) row[5]).longValue() : 0L;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("modelId", modelId);
            item.put("modelName", resolveModelName(modelId));
            item.put("taskCode", tc);
            item.put("avgLatencyMs", avgLatencyMs);
            item.put("successRate", successRate);
            item.put("avgTokens", avgTokens);
            item.put("totalCalls", totalCalls);
            item.put("source", "ai_model_benchmark");
            result.add(item);
        }
        return result;
    }

    @Override
    public Long selectBestModel(String taskCode, String priority) {
        List<Map<String, Object>> comparisons = getModelComparison(taskCode);
        if (comparisons == null || comparisons.isEmpty()) {
            return null;
        }
        String p = priority != null ? priority.trim().toLowerCase() : "latency";
        return switch (p) {
            case "tokens", "cost" -> comparisons.stream()
                    .min(Comparator.comparingDouble(m -> doubleValue(m.get("avgTokens"))))
                    .map(m -> ((Number) m.get("modelId")).longValue())
                    .orElse(null);
            case "success", "quality" -> comparisons.stream()
                    .max(Comparator.comparingDouble(m -> doubleValue(m.get("successRate"))))
                    .map(m -> ((Number) m.get("modelId")).longValue())
                    .orElse(null);
            default -> comparisons.stream()
                    .min(Comparator.comparingDouble(m -> doubleValue(m.get("avgLatencyMs"))))
                    .map(m -> ((Number) m.get("modelId")).longValue())
                    .orElse(null);
        };
    }

    @Override
    public Map<String, Object> recommendBestModel(String taskCode, String priority) {
        Map<String, Object> out = new LinkedHashMap<>();
        String tc = StringUtils.hasText(taskCode) ? taskCode.trim() : "default";
        String p = StringUtils.hasText(priority) ? priority.trim().toLowerCase() : "latency";
        out.put("taskCode", tc);
        out.put("priority", p);
        Long bestId = selectBestModel(tc, p);
        if (bestId == null) {
            out.put("modelId", 0L);
            out.put("modelName", "");
            return out;
        }
        out.put("modelId", bestId);
        out.put("modelName", resolveModelName(bestId));
        return out;
    }

    private String resolveModelName(Long modelId) {
        return modelRepository.findById(modelId)
                .map(AiModel::getModelName)
                .orElse("模型#" + modelId);
    }

    private Optional<Long> resolveModelId(String modelCode, String callType, String templateCode) {
        Optional<Long> explicitId = parseExplicitModelId(modelCode);
        if (explicitId.isPresent()) {
            return explicitId;
        }
        if (StringUtils.hasText(modelCode)) {
            String normalized = modelCode.trim();
            Optional<Long> byVersion = modelRepository.findByStatusAndDeleted(1, 0).stream()
                    .filter(model -> equalsIgnoreCase(model.getModelVersion(), normalized)
                            || equalsIgnoreCase(model.getModelName(), normalized))
                    .map(AiModel::getId)
                    .findFirst();
            if (byVersion.isPresent()) {
                return byVersion;
            }
        }
        return resolveConfiguredModelId(resolveTaskCode(callType, templateCode));
    }

    private Optional<Long> parseExplicitModelId(String modelCode) {
        if (!StringUtils.hasText(modelCode)) {
            return Optional.empty();
        }
        String value = modelCode.trim();
        if (value.startsWith("model:")) {
            value = value.substring("model:".length()).trim();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Long> resolveConfiguredModelId(String taskCode) {
        if (!StringUtils.hasText(taskCode)) {
            return Optional.empty();
        }
        return taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(taskCode, 1, 0)
                .flatMap(config -> Arrays.asList(config.getPrimaryModelId(), config.getFallbackModelId(), config.getFallback2ModelId())
                        .stream()
                        .filter(id -> id != null && modelRepository.findById(id).isPresent())
                        .findFirst());
    }

    private String resolveTaskCode(String callType, String templateCode) {
        if (StringUtils.hasText(templateCode) && taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(templateCode.trim(), 1, 0).isPresent()) {
            return templateCode.trim();
        }
        if (!StringUtils.hasText(callType)) {
            return null;
        }
        String value = callType.trim();
        if (taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(value, 1, 0).isPresent()) {
            return value;
        }
        if (value.startsWith("short_video_")) {
            return "short_video_script";
        }
        if (value.startsWith("live_script_") || "live_analysis".equals(value)) {
            return "copy_processing";
        }
        if ("kb_search".equals(value)) {
            return "kb_hyde";
        }
        return value;
    }

    private static int resolveTokens(AiCallLog log) {
        if (log.getTotalTokens() != null) {
            return Math.max(log.getTotalTokens(), 0);
        }
        int prompt = log.getPromptTokens() != null ? Math.max(log.getPromptTokens(), 0) : 0;
        int completion = log.getCompletionTokens() != null ? Math.max(log.getCompletionTokens(), 0) : 0;
        if (prompt + completion > 0) {
            return prompt + completion;
        }
        return log.getOutputLength() != null ? Math.max(log.getOutputLength(), 0) : 0;
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private static class BenchmarkAccumulator {
        private final Long modelId;
        private final String modelName;
        private final String taskCode;
        private double latencyWeightedSum;
        private double successWeightedSum;
        private double tokensWeightedSum;
        private long totalCalls;

        private BenchmarkAccumulator(Long modelId, String modelName, String taskCode) {
            this.modelId = modelId;
            this.modelName = modelName;
            this.taskCode = taskCode;
        }

        private void add(double avgLatencyMs, double successRate, double avgTokens, long calls) {
            latencyWeightedSum += avgLatencyMs * calls;
            successWeightedSum += successRate * calls;
            tokensWeightedSum += avgTokens * calls;
            totalCalls += calls;
        }

        private Map<String, Object> toMap() {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("modelId", modelId);
            item.put("modelName", modelName);
            item.put("taskCode", taskCode);
            item.put("avgLatencyMs", totalCalls > 0 ? latencyWeightedSum / totalCalls : 0.0);
            item.put("successRate", totalCalls > 0 ? successWeightedSum / totalCalls : 0.0);
            item.put("avgTokens", totalCalls > 0 ? tokensWeightedSum / totalCalls : 0.0);
            item.put("totalCalls", totalCalls);
            item.put("source", "ai_call_log");
            return item;
        }
    }

    private static double doubleValue(Object o) {
        if (o == null) {
            return 0.0;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }
}
