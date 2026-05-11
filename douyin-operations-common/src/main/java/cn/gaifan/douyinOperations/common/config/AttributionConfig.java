package cn.gaifan.douyinOperations.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 归因分析与优化配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.business.attribution")
public class AttributionConfig {

    // ── 话术归因权重 ──
    private Attribution attribution = new Attribution();

    // ── 效果评估权重 ──
    private Effectiveness effectiveness = new Effectiveness();

    // ── 版本对比阈值 ──
    private VersionComparison versionComparison = new VersionComparison();

    // ── 自动优化开关 ──
    private AutoImprove autoImprove = new AutoImprove();

    // ── 知识库质量评分权重 ──
    private KnowledgeQuality knowledgeQuality = new KnowledgeQuality();

    // ── 行业基准 ──
    private Map<String, IndustryBenchmark> industryBenchmark = Map.of(
            "skincare", new IndustryBenchmark(5000, 0.05, 0.35, 0.02),
            "cosmetics", new IndustryBenchmark(8000, 0.06, 0.30, 0.025)
    );

    // ── 趋势预测时间窗口 ──
    private TrendMonitor trendMonitor = new TrendMonitor();

    // ── 因果推理引擎 ──
    private CausalEngine causalEngine = new CausalEngine();

    @Data
    public static class Attribution {
        private double viewerWeight = 0.35;
        private double interactionWeight = 0.35;
        private double conversionWeight = 0.30;
        private double lowScoreThreshold = 40.0;
        private double criticalScoreThreshold = 25.0;
        /** 默认归因窗口（秒） */
        private int windowSeconds = 30;
        /** 品类关键字 → 归因窗口秒数 */
        private Map<String, Integer> windowSecondsByCategory = new LinkedHashMap<>();

        /**
         * 解析单场话术归因窗口：优先按商品类目匹配，否则返回默认值（夹在 5–600s）
         */
        public int resolveWindowSeconds(String categoryRaw) {
            int def = clampWindow(windowSeconds);
            if (categoryRaw == null || categoryRaw.isBlank()) {
                return def;
            }
            if (windowSecondsByCategory == null || windowSecondsByCategory.isEmpty()) {
                return def;
            }
            String t = categoryRaw.trim();
            Integer v = windowSecondsByCategory.get(t);
            if (v == null) {
                v = windowSecondsByCategory.get(t.toLowerCase(Locale.ROOT));
            }
            if (v == null) {
                for (Map.Entry<String, Integer> e : windowSecondsByCategory.entrySet()) {
                    if (e.getKey() != null && t.equalsIgnoreCase(e.getKey().trim())) {
                        v = e.getValue();
                        break;
                    }
                }
            }
            if (v == null || v <= 0) {
                return def;
            }
            return clampWindow(v);
        }

        private static int clampWindow(int sec) {
            return Math.max(5, Math.min(600, sec));
        }
    }

    @Data
    public static class Effectiveness {
        private double conversionWeight = 0.4;
        private double likeWeight = 0.3;
        private double commentWeight = 0.2;
        private double completionWeight = 0.1;
        private double maxScore = 10.0;
    }

    @Data
    public static class VersionComparison {
        private double highSimilarityThreshold = 0.95;
        private double lowSimilarityThreshold = 0.60;
    }

    @Data
    public static class AutoImprove {
        private boolean enabled = true;
        private boolean autoArchiveCritical = true;
    }

    @Data
    public static class KnowledgeQuality {
        private double qualityWeight = 0.25;
        private double effectivenessWeight = 0.30;
        private double freshnessWeight = 0.20;
        private double relevanceWeight = 0.15;
        private double usageWeight = 0.10;
    }

    @Data
    public static class IndustryBenchmark {
        private long avgViewCount;
        private double avgLikeRate;
        private double avgCompletionRate;
        private double avgShareRate;

        public IndustryBenchmark(long avgViewCount, double avgLikeRate, double avgCompletionRate, double avgShareRate) {
            this.avgViewCount = avgViewCount;
            this.avgLikeRate = avgLikeRate;
            this.avgCompletionRate = avgCompletionRate;
            this.avgShareRate = avgShareRate;
        }
    }

    @Data
    public static class TrendMonitor {
        private int shortWindowHours = 6;
        private int longWindowHours = 24;
        private int goldenWindowHours = 6;
        private int silverWindowHours = 24;
        private int bronzeWindowHours = 72;
        private double emergingMomentumThreshold = 0.5;
        private double risingMomentumThreshold = 0.1;
        private double decliningMomentumThreshold = -0.1;
    }

    @Data
    public static class CausalEngine {
        /** 贝叶斯先验：基准转化率 */
        private double baseRate = 0.35;
        /** 话术类型因子：匹配关键词 -> 乘数 */
        private Map<String, Double> scriptTypeFactors = Map.of(
                "种草", 1.15, "安利", 1.15,
                "促销", 1.08, "秒杀", 1.08,
                "产品", 1.05
        );
        /** 人设因子 */
        private Map<String, Double> personaFactors = Map.of(
                "专业", 1.12, "达人", 1.12,
                "亲切", 1.05, "姐妹", 1.05
        );
        /** 产品类型因子 */
        private Map<String, Double> productTypeFactors = Map.of(
                "护肤", 1.10, "美妆", 1.10, "精华", 1.12, "面膜", 1.08, "眼霜", 1.10, "套盒", 1.15
        );
        /** 时段因子 */
        private Map<String, Double> timeSlotFactors = Map.of(
                "晚", 1.08, "20", 1.08, "21", 1.08,
                "午", 1.02, "12", 1.02
        );
    }
}
