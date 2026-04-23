package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiModelBenchmark;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelBenchmarkRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.ModelBenchmarkService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelBenchmarkServiceImpl implements ModelBenchmarkService {

    @Resource
    private AiModelBenchmarkRepository benchmarkRepository;

    @Resource
    private AiModelRepository modelRepository;

    @Override
    public void recordBenchmark(Long modelId, String taskCode, long latencyMs, int tokensUsed, boolean success) {
        AiModelBenchmark benchmark = new AiModelBenchmark();
        benchmark.setModelId(modelId);
        benchmark.setTaskCode(taskCode);
        benchmark.setLatencyMs(latencyMs);
        benchmark.setTokensUsed(tokensUsed);
        benchmark.setSuccess(success);
        benchmarkRepository.save(benchmark);
    }

    @Override
    public List<Map<String, Object>> getModelComparison(String taskCode) {
        List<Object[]> rows = StringUtils.hasText(taskCode)
                ? benchmarkRepository.aggregateByTaskCode(taskCode.trim())
                : benchmarkRepository.aggregateAllByModelAndTask();
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
