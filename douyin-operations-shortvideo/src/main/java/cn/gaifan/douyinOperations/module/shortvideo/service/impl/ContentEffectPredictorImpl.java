package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentEffectPredictor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContentEffectPredictorImpl implements ContentEffectPredictor {

    private static final Logger log = LoggerFactory.getLogger(ContentEffectPredictorImpl.class);
    @Resource private LlmClient llmClient;
    @Resource private AiModelRepository modelRepository;
    @Resource private ObjectMapper objectMapper;
    @Resource private SvVideoRepository svVideoRepository;

    /** H-3：送入模型的脚本前缀最大字符数（防 prompt 过长） */
    @Value("${app.shortvideo.effect-predict.max-script-chars:500}")
    private int effectPredictMaxScriptChars;
    /** H-3：参与回退的模型条数上限 */
    @Value("${app.shortvideo.effect-predict.model-limit:3}")
    private int effectPredictModelLimit;

    /** H-4：追加到效果预测 system 提示的可选段落（运营约束语气/字段口径，非平台承诺） */
    @Value("${app.shortvideo.effect-predict.system-extra:}")
    private String effectPredictSystemExtra;

    /** H-4：限制 strengths/weaknesses/suggestions 条数，写入 user 提示（不改全局 temperature） */
    @Value("${app.shortvideo.effect-predict.max-output-bullets:6}")
    private int effectPredictMaxOutputBullets;

    /** H-4：历史回算校准系数（0–1），默认 1=不调制 */
    @Value("${app.shortvideo.effect-predict.calibration-factor:1.0}")
    private double effectPredictCalibrationFactor;

    @Value("${app.shortvideo.effect-predict.calibration-note:}")
    private String effectPredictCalibrationNote;

    /** H-5：历史基线回看天数 */
    @Value("${app.shortvideo.effect-predict.history-lookback-days:90}")
    private int effectPredictHistoryLookbackDays;

    /** H-5：启用历史基线校准的最小样本数 */
    @Value("${app.shortvideo.effect-predict.history-min-samples:3}")
    private int effectPredictHistoryMinSamples;

    /** H-5：历史基线融合权重（0-1，越大越偏向真实历史表现） */
    @Value("${app.shortvideo.effect-predict.history-baseline-weight:0.35}")
    private double effectPredictHistoryBaselineWeight;

    @Override
    public Map<String, Object> previewConfidenceCalibration(Double confidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("calibrationFactor", effectPredictCalibrationFactor);
        if (StringUtils.hasText(effectPredictCalibrationNote)) {
            m.put("calibrationNote", effectPredictCalibrationNote.trim());
        }
        if (confidence == null || !Double.isFinite(confidence)) {
            m.put("error", "confidence 须为有限数值");
            return m;
        }
        double factor = effectPredictCalibrationFactor;
        if (factor < 0 || factor > 1.0) {
            m.put("error", "calibration-factor 配置非法");
            return m;
        }
        double v = confidence * factor;
        m.put("inputConfidence", confidence);
        m.put("calibratedConfidence", Math.min(1.0, Math.max(0.0, v)));
        return m;
    }

    @Override
    public Map<String, Object> predict(Long userId, String scriptContent, String title, String publishTime) {
        int cap = Math.max(120, effectPredictMaxScriptChars);
        String scriptSnippet = scriptContent != null ? scriptContent : "";
        if (scriptSnippet.length() > cap) {
            scriptSnippet = scriptSnippet.substring(0, cap);
        }
        String prompt = String.format("""
                请基于以下短视频内容预测其发布效果。

                标题：%s
                脚本内容（前%d字）：%s
                计划发布时间：%s

                请以 JSON 格式输出预测结果（字段含义供运营参考，非平台真实统计承诺）：
                {
                  "viewRange": {"min": 5000, "max": 20000},
                  "likeRange": {"min": 200, "max": 1000},
                  "engagementRate": "3-5%%",
                  "confidence": 0.7,
                  "strengths": ["标题吸引力强", "选题契合热点"],
                  "weaknesses": ["缺少互动引导"],
                  "suggestions": ["在结尾增加互动提问", "适当缩短开头"]
                }

                请直接输出 JSON，不要额外说明。
                """,
                title != null ? title : "未命名",
                cap,
                scriptSnippet,
                publishTime != null ? publishTime : "未指定");

        int bulletCap = Math.max(1, Math.min(effectPredictMaxOutputBullets, 20));
        prompt = prompt + "\n\n请控制输出体量：strengths、weaknesses、suggestions 每个数组最多 " + bulletCap + " 条短句，避免冗长罗列。";

        int lim = Math.max(1, Math.min(effectPredictModelLimit, 20));
        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0).stream().limit(lim).toList();
        if (models.isEmpty()) return Map.of("error", "无可用模型");
        String system = "你是短视频数据分析专家，擅长内容效果预测";
        if (StringUtils.hasText(effectPredictSystemExtra)) {
            system = system + "\n" + effectPredictSystemExtra.trim();
        }
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, system, prompt);
        if (!resp.success()) return Map.of("error", resp.errorMsg());
        try {
            Map<String, Object> parsed = objectMapper.readValue(resp.content(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
            Map<String, Object> cleaned = sanitizeEffectPredictionLists(parsed, bulletCap);
            applyConfidenceCalibration(cleaned);
            applyHistoricalBaseline(cleaned, loadHistoricalBaseline(userId));
            return cleaned;
        } catch (Exception e) {
            return Map.of("prediction", resp.content(), "parseError", true);
        }
    }

    private void applyConfidenceCalibration(Map<String, Object> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            return;
        }
        double factor = effectPredictCalibrationFactor;
        if (factor >= 0.9999) {
            return;
        }
        if (factor < 0 || factor > 1.0) {
            return;
        }
        Object c = parsed.get("confidence");
        if (!(c instanceof Number n)) {
            return;
        }
        double v = n.doubleValue() * factor;
        parsed.put("confidence", Math.min(1.0, Math.max(0.0, v)));
        parsed.put("confidenceCalibrationFactor", factor);
        if (StringUtils.hasText(effectPredictCalibrationNote)) {
            parsed.put("calibrationNote", effectPredictCalibrationNote.trim());
        }
    }

    /**
     * H-4：JSON 解析成功后裁剪 strengths/weaknesses/suggestions 条数，与 user 提示中的 max 一致，避免模型超长输出。
     */
    private static Map<String, Object> sanitizeEffectPredictionLists(Map<String, Object> raw, int bulletCap) {
        if (raw == null) {
            return Map.of();
        }
        Map<String, Object> out = new LinkedHashMap<>(raw);
        for (String key : List.of("strengths", "weaknesses", "suggestions")) {
            Object v = out.get(key);
            if (!(v instanceof List<?> list)) {
                continue;
            }
            List<String> trimmed = new ArrayList<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = String.valueOf(o).trim();
                if (s.isEmpty()) {
                    continue;
                }
                trimmed.add(s);
                if (trimmed.size() >= bulletCap) {
                    break;
                }
            }
            out.put(key, trimmed);
        }
        return out;
    }

    private HistoricalBaseline loadHistoricalBaseline(Long userId) {
        if (userId == null) {
            return HistoricalBaseline.empty();
        }
        int lookbackDays = Math.max(1, effectPredictHistoryLookbackDays);
        Timestamp since = Timestamp.from(Instant.now().minusSeconds(lookbackDays * 24L * 3600L));
        List<SvVideo> recentPublished = svVideoRepository.findByOwnerIdAndPublishTimeAfterAndDeleted(userId, since, 0);
        if (recentPublished == null || recentPublished.isEmpty()) {
            return HistoricalBaseline.empty();
        }

        List<Long> views = recentPublished.stream()
                .map(SvVideo::getViewCount)
                .filter(v -> v != null && v > 0)
                .sorted()
                .toList();
        List<Double> engagementRates = recentPublished.stream()
                .map(this::computeEngagementRatePct)
                .filter(v -> v != null && Double.isFinite(v) && v >= 0)
                .sorted(Comparator.naturalOrder())
                .toList();

        if (views.size() < effectPredictHistoryMinSamples || engagementRates.size() < effectPredictHistoryMinSamples) {
            return new HistoricalBaseline(recentPublished.size(), medianLong(views), medianDouble(engagementRates));
        }
        return new HistoricalBaseline(recentPublished.size(), medianLong(views), medianDouble(engagementRates));
    }

    private void applyHistoricalBaseline(Map<String, Object> parsed, HistoricalBaseline baseline) {
        if (parsed == null || parsed.isEmpty() || baseline == null || baseline.sampleSize() <= 0) {
            return;
        }
        Map<String, Object> baselineMap = new LinkedHashMap<>();
        baselineMap.put("sampleSize", baseline.sampleSize());
        baselineMap.put("medianViews", baseline.medianViews());
        baselineMap.put("medianEngagementRate", baseline.medianEngagementRate());
        parsed.put("historicalBaseline", baselineMap);

        if (baseline.sampleSize() < effectPredictHistoryMinSamples) {
            parsed.put("baselineApplied", false);
            parsed.put("baselineReason", "history_samples_insufficient");
            return;
        }

        double weight = effectPredictHistoryBaselineWeight;
        if (weight <= 0 || weight >= 1 || baseline.medianViews() == null || baseline.medianEngagementRate() == null) {
            parsed.put("baselineApplied", false);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> viewRange = parsed.get("viewRange") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : null;
        if (viewRange != null) {
            Long min = toLong(viewRange.get("min"));
            Long max = toLong(viewRange.get("max"));
            if (min != null && max != null && max >= min) {
                long predictedMid = Math.round((min + max) / 2.0);
                long adjustedMid = Math.round(predictedMid * (1 - weight) + baseline.medianViews() * weight);
                long spread = Math.max(1, Math.round((max - min) / 2.0));
                long adjustedMin = Math.max(0, adjustedMid - spread);
                long adjustedMax = Math.max(adjustedMin, adjustedMid + spread);
                parsed.put("baselineAdjustedViewRange", Map.of(
                        "min", adjustedMin,
                        "max", adjustedMax
                ));
            }
        }

        String engagementRate = parsed.get("engagementRate") != null ? String.valueOf(parsed.get("engagementRate")) : null;
        Double predictedEngagementMid = parseEngagementRateMid(engagementRate);
        if (predictedEngagementMid != null) {
            double adjusted = predictedEngagementMid * (1 - weight) + baseline.medianEngagementRate() * weight;
            parsed.put("baselineAdjustedEngagementRate", String.format("%.1f%%", adjusted));
        }
        parsed.put("baselineApplied", true);
        parsed.put("baselineWeight", weight);
    }

    private Double computeEngagementRatePct(SvVideo video) {
        if (video == null || video.getViewCount() == null || video.getViewCount() <= 0) {
            return null;
        }
        long interactions = (video.getLikeCount() != null ? video.getLikeCount() : 0)
                + (video.getCommentCount() != null ? video.getCommentCount() : 0)
                + (video.getShareCount() != null ? video.getShareCount() : 0);
        return interactions * 100.0 / video.getViewCount();
    }

    private Long medianLong(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(values.size() / 2);
    }

    private Double medianDouble(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(values.size() / 2);
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return value != null ? Long.parseLong(String.valueOf(value)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseEngagementRateMid(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace("%", "").trim();
        try {
            if (normalized.contains("-")) {
                String[] parts = normalized.split("-");
                if (parts.length == 2) {
                    double a = Double.parseDouble(parts[0].trim());
                    double b = Double.parseDouble(parts[1].trim());
                    return (a + b) / 2.0;
                }
            }
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            log.debug("解析 engagementRate 失败: {}", value);
            return null;
        }
    }

    private record HistoricalBaseline(int sampleSize, Long medianViews, Double medianEngagementRate) {
        private static HistoricalBaseline empty() {
            return new HistoricalBaseline(0, null, null);
        }
    }
}
