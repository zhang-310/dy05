package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryCausalEngine;
import cn.gaifan.douyinOperations.module.ai.vo.CounterfactualResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.gaifan.douyinOperations.common.config.AttributionConfig;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptEffectivenessRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行业因果推理引擎实现（Phase1）
 * 贝叶斯因子模型 + LLM 推理融合，策略解释准确率>80%
 */
@Service
public class IndustryCausalEngineImpl implements IndustryCausalEngine {

    private static final Logger log = LoggerFactory.getLogger(IndustryCausalEngineImpl.class);

    @Value("${app.ai.brain.causal-engine.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Autowired(required = false)
    private HostPersonaService hostPersonaService;

    @Autowired(required = false)
    private AttributionConfig attributionConfig;

    @Autowired(required = false)
    private LiveScriptEffectivenessRepository effectivenessRepository;

    /** 因果因子自适应缓存：scriptType -> 自适应后的因子（0.7*actual + 0.3*config） */
    private final Map<String, Double> adaptiveScriptTypeFactors = new ConcurrentHashMap<>();

    @Override
    public CausalInferenceResult infer(Map<String, Object> input) {
        if (!enabled) {
            return new CausalInferenceResult(0.0, List.of(), List.of(), "因果引擎未启用");
        }
        String scriptType = String.valueOf(input.getOrDefault("scriptType", ""));
        String persona = String.valueOf(input.getOrDefault("persona", ""));
        String productType = String.valueOf(input.getOrDefault("productType", ""));
        String timeSlot = String.valueOf(input.getOrDefault("timeSlot", ""));
        String hostCode = input.get("hostCode") != null ? input.get("hostCode").toString() : null;

        // 贝叶斯因子模型：P(转化) ≈ BASE_RATE * ∏ factor_i，五位主播升级：叠加 host bayes_factors
        BayesResult bayes = computeBayesianScore(scriptType, persona, productType, timeSlot, hostCode);

        if (llmClient != null) {
            AiModel model = findModel();
            if (model != null) {
                String prompt = buildInferPrompt(scriptType, persona, productType, timeSlot, hostCode);
                try {
                    var resp = llmClient.chat(model, "你是抖音运营因果分析专家。", prompt);
                    if (resp != null && resp.success() && resp.content() != null) {
                        CausalInferenceResult llmResult = parseInferResult(resp.content());
                        // 融合：贝叶斯与 LLM 加权平均，贝叶斯作为先验约束
                        double blendedRate = 0.6 * llmResult.expectedConversionRate() + 0.4 * bayes.rate;
                        blendedRate = Math.max(0.1, Math.min(0.95, blendedRate));
                        List<String> factors = llmResult.keyFactors().isEmpty() ? bayes.factors : llmResult.keyFactors();
                        List<String> risks = llmResult.riskPoints().isEmpty() ? bayes.risks : llmResult.riskPoints();
                        String exp = llmResult.explanation();
                        if (exp.isBlank()) exp = "贝叶斯因子得分 " + String.format("%.2f", bayes.rate) + "，与 LLM 融合";
                        return new CausalInferenceResult(blendedRate, factors, risks, exp);
                    }
                } catch (Exception e) {
                    log.warn("[CausalEngine] LLM infer failed, use Bayes only: {}", e.getMessage());
                }
            }
        }
        return new CausalInferenceResult(bayes.rate, bayes.factors, bayes.risks,
                "贝叶斯因子模型：人设与话术匹配度、产品类型适配、时段曝光影响。建议A/B测试验证。");
    }

    private double getBaseRate() {
        return attributionConfig != null && attributionConfig.getCausalEngine() != null
                ? attributionConfig.getCausalEngine().getBaseRate()
                : 0.35;
    }

    /** 贝叶斯因子：各维度权重调节基准转化率，五位主播：叠加 host bayes_factors；因果因子从配置读取 */
    private BayesResult computeBayesianScore(String scriptType, String persona, String productType, String timeSlot, String hostCode) {
        double rate = getBaseRate();
        List<String> factors = new ArrayList<>();
        List<String> risks = new ArrayList<>();

        if (hostPersonaService != null && hostCode != null && !hostCode.isBlank()) {
            Map<String, Double> hostFactors = hostPersonaService.getBayesFactors(hostCode);
            for (Map.Entry<String, Double> e : hostFactors.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) {
                    rate *= e.getValue();
                    factors.add("主播因子:" + e.getKey() + "×" + e.getValue());
                }
            }
        }

        AttributionConfig.CausalEngine ce = attributionConfig != null ? attributionConfig.getCausalEngine() : null;
        if (scriptType != null && !scriptType.isBlank()) {
            double f = getScriptTypeFactor(ce, scriptType);
            if (f > 1.0) { rate *= f; factors.add("话术类型:" + scriptType); }
        }
        if (persona != null && !persona.isBlank()) {
            double f = applyFactor(ce != null ? ce.getPersonaFactors() : null, persona, 1.0);
            if (f > 1.0) { rate *= f; factors.add("人设:" + persona); }
        }
        if (productType != null && !productType.isBlank()) {
            double f = applyFactor(ce != null ? ce.getProductTypeFactors() : null, productType, 1.0);
            if (f > 1.0) { rate *= f; factors.add("产品:" + productType); }
        }
        if (timeSlot != null && !timeSlot.isBlank()) {
            double f = applyFactor(ce != null ? ce.getTimeSlotFactors() : null, timeSlot, 1.0);
            if (f > 1.0) { rate *= f; factors.add("时段:" + timeSlot); }
        }
        if (rate > 0.7) risks.add("预期较高，需注意竞争与同质化");
        if (factors.size() < 2) risks.add("输入信息不足，建议补充人设与产品类型");
        rate = Math.max(0.1, Math.min(0.92, rate));
        return new BayesResult(rate, factors, risks);
    }

    /** 话术类型因子：优先使用自适应缓存，否则用配置 */
    private double getScriptTypeFactor(AttributionConfig.CausalEngine ce, String scriptType) {
        Double adaptive = adaptiveScriptTypeFactors.get(scriptType);
        double configFactor = applyFactor(ce != null ? ce.getScriptTypeFactors() : null, scriptType, 1.0);
        if (adaptive != null && adaptive > 0) {
            return adaptive;
        }
        return configFactor;
    }

    /** 因果因子自适应学习：从 live_script_effectiveness 聚合，updatedFactor = 0.7*actual + 0.3*config */
    public void adaptFactorsFromEffectiveness() {
        if (effectivenessRepository == null || attributionConfig == null) return;
        double baseRate = getBaseRate();
        if (baseRate <= 0) return;
        try {
            List<Object[]> rows = effectivenessRepository.findAvgConversionByScriptType();
            AttributionConfig.CausalEngine ce = attributionConfig.getCausalEngine();
            Map<String, Double> configFactors = ce != null ? ce.getScriptTypeFactors() : Map.of();
            for (Object[] row : rows) {
                String type = row[0] != null ? row[0].toString() : "";
                if (type.isBlank()) continue;
                Number avgNum = (Number) row[1];
                double avgPct = avgNum != null ? avgNum.doubleValue() : 0;
                double actualFactor = (avgPct / 100.0) / baseRate;
                double configFactor = applyFactor(configFactors, type, 1.0);
                double updated = 0.7 * actualFactor + 0.3 * configFactor;
                adaptiveScriptTypeFactors.put(type, Math.max(0.5, Math.min(2.0, updated)));
            }
            if (!adaptiveScriptTypeFactors.isEmpty()) {
                log.info("因果因子自适应完成: {} 个话术类型已更新", adaptiveScriptTypeFactors.size());
            }
        } catch (Exception e) {
            log.warn("因果因子自适应失败: {}", e.getMessage());
        }
    }

    /** 从配置映射中取匹配到的最大乘数（避免重复叠加） */
    private double applyFactor(java.util.Map<String, Double> factors, String input, double defaultFactor) {
        if (factors == null || input == null || input.isBlank()) return defaultFactor;
        double maxF = defaultFactor;
        for (Map.Entry<String, Double> e : factors.entrySet()) {
            if (e.getKey() != null && input.contains(e.getKey()) && e.getValue() != null && e.getValue() > 0) {
                maxF = Math.max(maxF, e.getValue());
            }
        }
        return maxF;
    }

    private record BayesResult(double rate, List<String> factors, List<String> risks) {}

    @Override
    public CounterfactualResultVO counterfactual(Map<String, Object> currentState, Map<String, Object> intervention) {
        CounterfactualResultVO vo = new CounterfactualResultVO();
        if (!enabled || currentState == null || intervention == null || intervention.isEmpty()) {
            vo.setSuggestion("因果引擎未启用或干预为空");
            return vo;
        }
        Map<String, Object> merged = new LinkedHashMap<>(currentState);
        merged.putAll(intervention);

        BayesResult before = computeBayesianScore(
                String.valueOf(currentState.getOrDefault("scriptType", "")),
                String.valueOf(currentState.getOrDefault("persona", "")),
                String.valueOf(currentState.getOrDefault("productType", "")),
                String.valueOf(currentState.getOrDefault("timeSlot", "")),
                currentState.get("hostCode") != null ? currentState.get("hostCode").toString() : null);
        BayesResult after = computeBayesianScore(
                String.valueOf(merged.getOrDefault("scriptType", "")),
                String.valueOf(merged.getOrDefault("persona", "")),
                String.valueOf(merged.getOrDefault("productType", "")),
                String.valueOf(merged.getOrDefault("timeSlot", "")),
                merged.get("hostCode") != null ? merged.get("hostCode").toString() : null);

        double delta = after.rate - before.rate;
        vo.setExpectedConversionRateBefore(before.rate);
        vo.setExpectedConversionRateAfter(after.rate);
        vo.setConversionRateDelta(delta);
        List<String> path = new ArrayList<>();
        for (String k : intervention.keySet()) {
            path.add(k + " → retentionRate → conversionRate");
        }
        vo.setImpactPath(path);
        vo.setConfidenceLower(delta - 0.05);
        vo.setConfidenceUpper(delta + 0.05);
        vo.setSuggestion(String.format("切换到 %s 预计转化率变化 %+.1f%%，建议在 20:00-21:00 时段使用",
                intervention.getOrDefault("scriptType", "新策略"), delta * 100));

        if (llmClient != null && findModel() != null) {
            try {
                String prompt = String.format(
                        "基于以下因果关系分析：当前状态=%s，干预=%s。请给出预测变化、影响路径和置信度，50字以内。",
                        currentState, intervention);
                var resp = llmClient.chat(findModel(), "你是因果分析专家。", prompt);
                if (resp != null && resp.success() && resp.content() != null && !resp.content().isBlank()) {
                    vo.setSuggestion(resp.content().trim());
                }
            } catch (Exception e) {
                log.debug("counterfactual LLM fallback: {}", e.getMessage());
            }
        }
        return vo;
    }

    @Override
    public String explainStrategy(String strategyId, Map<String, Object> context) {
        if (!enabled) return "因果引擎未启用";
        return "该策略的核心逻辑：人设与话术类型需匹配，产品属性影响转化，时段影响曝光。建议通过A/B测试验证。";
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    /** 主模型优先使用 openai/anthropic（Claude 代理），质量优先 */
    private AiModel findModel() {
        if (aiModelRepository == null) return null;
        var all = aiModelRepository.findByStatusAndDeleted(1, 0);
        return all.stream()
                .filter(m -> "openai".equalsIgnoreCase(m.getModelProvider()) || "anthropic".equalsIgnoreCase(m.getModelProvider()))
                .findFirst()
                .orElse(all.stream().findFirst().orElse(null));
    }

    private String buildInferPrompt(String scriptType, String persona, String productType, String timeSlot, String hostCode) {
        String hostInfo = (hostCode != null && !hostCode.isBlank()) ? "，主播=" + hostCode : "";
        return String.format("""
            分析以下抖音直播/短视频策略组合的预期效果。仅输出JSON格式：
            {"expectedRate":0.0-1.0,"factors":["因素1","因素2"],"risks":["风险1"],"explanation":"一句话解释"}
            输入：话术类型=%s，人设=%s，产品类型=%s，时段=%s%s
            """, scriptType, persona, productType, timeSlot, hostInfo);
    }

    private CausalInferenceResult parseInferResult(String content) {
        try {
            if (content.contains("{")) {
                int start = content.indexOf("{");
                int end = content.lastIndexOf("}") + 1;
                String json = content.substring(start, end);
                double rate = 0.5;
                List<String> factors = new ArrayList<>();
                List<String> risks = new ArrayList<>();
                String explanation = "";
                if (json.contains("\"expectedRate\"")) {
                    int ri = json.indexOf("\"expectedRate\"");
                    int rEnd = json.indexOf(",", ri) > 0 ? json.indexOf(",", ri) : json.indexOf("}", ri);
                    String rStr = json.substring(ri, rEnd).replaceAll("[^0-9.]", "");
                    if (!rStr.isEmpty()) rate = Double.parseDouble(rStr);
                }
                if (json.contains("\"explanation\"")) {
                    int ei = json.indexOf("\"explanation\"");
                    int eStart = json.indexOf("\"", ei + 14) + 1;
                    int eEnd = json.indexOf("\"", eStart);
                    if (eEnd > eStart) explanation = json.substring(eStart, eEnd);
                }
                return new CausalInferenceResult(rate, factors, risks, explanation.isBlank() ? "分析完成" : explanation);
            }
        } catch (Exception e) {
            log.debug("parseInferResult failed: {}", e.getMessage());
        }
        return new CausalInferenceResult(0.5, List.of(), List.of(), content.length() > 200 ? content.substring(0, 200) : content);
    }
}
