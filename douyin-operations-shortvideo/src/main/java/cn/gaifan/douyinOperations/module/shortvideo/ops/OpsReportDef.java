package cn.gaifan.douyinOperations.module.shortvideo.ops;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * N-5：运营报表模板 {@code sv_ops_report_template.definition_json} 的轻量解析（文档即契约）。
 */
public final class OpsReportDef {

    /** 当月 {@code publishedCount / plannedCount} 低于该比例时触发 completionGap 规则（默认 0.5）。 */
    public double completionGapRatio = 0.5d;
    /** 启发式峰值与统计峰值绝对差超过该值时触发 forecastDivergence（默认 25）。 */
    public double forecastDivergenceScore = 25d;
    /** 是否在 rulesSummary 中附带日峰值列表。 */
    public boolean includeDailyPeaks = false;
    /** 日峰值 top-N（1～31）。 */
    public int dailyPeaksTopN = 5;
    /** CSV 导出额外 metric 键名（从 stats / 双 forecast 中取，见 {@link #parse} 契约说明）。 */
    public List<String> csvExtraMetrics = List.of();

    public static OpsReportDef defaults() {
        return new OpsReportDef();
    }

    /**
     * definition_json 约定字段：
     * <ul>
     *   <li>{@code extraRuleThresholds}: {@code { "completionGapRatio": 0.5, "forecastDivergenceScore": 25 }}</li>
     *   <li>{@code includeDailyPeaks}: boolean</li>
     *   <li>{@code dailyPeaksTopN}: int</li>
     *   <li>{@code csvExtraMetrics}: string[]，键名为 stats / forecast 响应中的字段名或可识别别名</li>
     * </ul>
     */
    public static OpsReportDef parse(Object definitionJson) {
        OpsReportDef d = defaults();
        if (definitionJson == null) {
            return d;
        }
        JSONObject root;
        if (definitionJson instanceof String s) {
            if (s.isBlank()) {
                return d;
            }
            root = JSON.parseObject(s);
        } else if (definitionJson instanceof Map<?, ?> m) {
            root = new JSONObject(m);
        } else {
            return d;
        }
        if (root == null || root.isEmpty()) {
            return d;
        }
        JSONObject thresholds = root.getJSONObject("extraRuleThresholds");
        if (thresholds != null) {
            Double c = thresholds.getDouble("completionGapRatio");
            if (c != null && c > 0 && c <= 1) {
                d.completionGapRatio = c;
            }
            Double f = thresholds.getDouble("forecastDivergenceScore");
            if (f != null && f >= 0) {
                d.forecastDivergenceScore = f;
            }
        }
        if (root.containsKey("includeDailyPeaks")) {
            d.includeDailyPeaks = Boolean.TRUE.equals(root.getBoolean("includeDailyPeaks"));
        }
        Integer topN = root.getInteger("dailyPeaksTopN");
        if (topN != null && topN > 0) {
            d.dailyPeaksTopN = Math.min(31, topN);
        }
        JSONArray arr = root.getJSONArray("csvExtraMetrics");
        if (arr != null && !arr.isEmpty()) {
            List<String> keys = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                String k = arr.getString(i);
                if (k != null && !k.isBlank()) {
                    keys.add(k.trim());
                }
            }
            d.csvExtraMetrics = keys.isEmpty() ? List.of() : Collections.unmodifiableList(keys);
        }
        return d;
    }
}
