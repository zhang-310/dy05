package cn.gaifan.douyinOperations.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 业务参数集中配置：将文档定义的阈值、频率、时间分配等统一管理。
 * 通过 application.yml 的 app.business 前缀覆盖。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.business")
public class BusinessParamConfig {

    // ── 爆款识别阈值 ──
    private ViralThreshold viral = new ViralThreshold();

    // ── 热点时间窗口 ──
    private HotspotWindow hotspot = new HotspotWindow();

    // ── 30分钟标准段时间分配（秒）──
    private LiveSegment liveSegment = new LiveSegment();

    // ── 留人策略频率（分钟）──
    private RetentionFrequency retention = new RetentionFrequency();

    // ── 价值塑造公式时间分配（秒）──
    private ValueFormula valueFormula = new ValueFormula();

    // ── IP成长阶段粉丝数阈值 ──
    private IpGrowth ipGrowth = new IpGrowth();

    // ── 二创SOP时间约束（分钟）──
    private RemakeSop remakeSop = new RemakeSop();

    // ── BGM音量参数 ──
    private BgmVolume bgmVolume = new BgmVolume();

    @Data
    public static class ViralThreshold {
        private long minViewCount = 10_000_000;
        private double minLikeRate = 0.10;
        private double minCompletionRate = 0.60;
        private double minShareRate = 0.05;
    }

    @Data
    public static class HotspotWindow {
        private int goldDays = 3;
        private int silverDays = 7;
        private int bronzeDays = 15;
    }

    @Data
    public static class LiveSegment {
        private int openingSec = 180;
        private int mainTopicSec = 900;
        private int interactionSec = 480;
        private int closingSec = 240;
    }

    @Data
    public static class RetentionFrequency {
        private int suspenseMinutes = 5;
        private int miniClimaxMinutes = 3;
        private int practicalTipMinutes = 10;
    }

    @Data
    public static class ValueFormula {
        private PhenomenalValue phenomenal = new PhenomenalValue();
        private TopValue top = new TopValue();

        @Data
        public static class PhenomenalValue {
            private int painPointSec = 30;
            private int solutionSec = 45;
            private int effectSec = 30;
            private int endorsementSec = 15;
            private int priceSec = 30;
        }

        @Data
        public static class TopValue {
            private int problemSec = 60;
            private int solutionSec = 90;
            private int dataSec = 60;
            private int techSec = 45;
            private int valueSec = 45;
        }
    }

    @Data
    public static class IpGrowth {
        private long phenomenalSeedMax = 100_000;
        private long phenomenalBoostMax = 500_000;
        private long phenomenalStableMax = 1_000_000;
        private int topFoundationMonths = 6;
        private int topExpansionMonths = 12;
        private int topEcosystemMonths = 24;
    }

    @Data
    public static class RemakeSop {
        private int totalTimeMinutes = 120;
        private int discoveryMinutes = 30;
        private int deconstructMinutes = 20;
        private int adaptMinutes = 20;
        private int shootMinutes = 30;
        private int publishMinutes = 20;
    }

    @Data
    public static class BgmVolume {
        private int frontVolumeMin = 60;
        private int frontVolumeMax = 70;
        private int backVolumeMin = 80;
        private int backVolumeMax = 90;
    }

    // ── 现象级/顶级 IP 运营指标阈值 ──
    private IpMetrics ipMetrics = new IpMetrics();

    // ── 短视频分镜推荐规则 ──
    private ShotCountRule shotCountRule = new ShotCountRule();

    // ── 情绪曲线模板 ──
    private EmotionCurve emotionCurve = new EmotionCurve();

    @Data
    public static class IpMetrics {
        private PhenomenalMetrics phenomenal = new PhenomenalMetrics();
        private TopMetrics top = new TopMetrics();

        @Data
        public static class PhenomenalMetrics {
            private long dailyFollowerGrowth = 50_000;
            private long minViewPerVideo = 5_000_000;
            private double minInteractionRate = 0.10;
            private int liveWatchMinutes = 40;
            private double liveConversionRate = 0.05;
            private int avgUnitPrice = 125;
        }

        @Data
        public static class TopMetrics {
            private long monthlyFollowerGrowth = 200_000;
            private double minRepurchaseRate = 0.40;
            private int avgUnitPrice = 500;
            private int customerLtv = 2000;
            private int liveWatchMinutes = 135;
            private double liveConversionRate = 0.02;
        }
    }

    @Data
    public static class ShotCountRule {
        private int sec15Shots = 5;
        private int sec30Shots = 7;
        private int sec45Shots = 9;
        private int sec60Shots = 12;
    }

    @Data
    public static class EmotionCurve {
        private String phenomenalCurve = "100→80→60→120→100→90→130→100";
        private String topCurve = "70→75→80→85→90→85→95→80";
        private String phenomenalDesc = "高开→微降→蓄力→爆发→缓冲→蓄力→最高潮→温暖收尾";
        private String topDesc = "稳开→渐升→专业→深入→高潮→回落→升华→沉淀收尾";
    }

    /** 直播讲解库存提示：注入 LLM user prompt + 实时建议（P1 LIVE-03） */
    private LiveInventory liveInventory = new LiveInventory();

    /** 实时话术建议阈值（Phase 2.4） */
    private RealtimeSuggestion realtimeSuggestion = new RealtimeSuggestion();

    @Data
    public static class RealtimeSuggestion {
        private boolean enabled = true;
        private int evaluateIntervalSeconds = 30;
        private int minRetentionRate = 40;
        private int minInteractionRate = 2;
        private int viewerDeclineMinutes = 3;
        private int minConversionRate = 1;
        private int maxExplainMinutes = 3;
    }

    /** 短视频情绪曲线模板（Phase 2.1）：code -> curveExpression */
    private Map<String, String> emotionCurveTemplates = Map.of(
            "hook_climax", "90→60→40→80→100→70",
            "slow_build", "40→50→60→75→90→100",
            "rollercoaster", "80→40→90→30→100→60",
            "suspense", "60→70→50→40→30→100",
            "emotional_wave", "70→90→50→85→40→95"
    );

    // ── 话术归因权重与自动优化（Phase 1 任务 1.6）──
    private Attribution attribution = new Attribution();
    private Effectiveness effectiveness = new Effectiveness();
    private VersionComparison versionComparison = new VersionComparison();
    private AutoImprove autoImprove = new AutoImprove();
    private KnowledgeQuality knowledgeQuality = new KnowledgeQuality();

    @Data
    public static class LiveInventory {
        /** 是否在 LLM 生成话术的 user prompt 中追加库存提醒 */
        private boolean promptHintEnabled = true;
        /** 是否在实时辅助面板建议中提示低库存 */
        private boolean realtimeSuggestionEnabled = true;
        /** 库存 ≤ 此值视为偏紧（Long，与 dy_product.inventory 一致） */
        private long lowStockThreshold = 50;
        /** 库存 ≤ 此值视为极低 */
        private long criticalStockThreshold = 10;
    }

    @Data
    public static class Attribution {
        private double viewerWeight = 0.35;
        private double interactionWeight = 0.35;
        private double conversionWeight = 0.30;
        private double lowScoreThreshold = 40.0;
        private double criticalScoreThreshold = 25.0;
        /** 默认归因窗口（秒）；按品类覆盖见 {@link #windowSecondsByCategory} */
        private int windowSeconds = 30;
        /**
         * 品类关键字 → 归因窗口秒数（键可为中文或英文，如 护肤、skincare）。
         * 未命中时使用 {@link #windowSeconds}。
         */
        private Map<String, Integer> windowSecondsByCategory = new LinkedHashMap<>();

        /**
         * 解析单场话术归因窗口：优先按商品类目匹配 {@code windowSecondsByCategory}，否则返回 {@code windowSeconds}（夹在 5–600s）。
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

    // ── 行业基准（账号诊断 Phase 3.6）──
    private Map<String, IndustryBenchmark> industryBenchmark = Map.of(
            "skincare", new IndustryBenchmark(5000, 0.05, 0.35, 0.02),
            "cosmetics", new IndustryBenchmark(8000, 0.06, 0.30, 0.025)
    );

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

    // ── 趋势预测时间窗口（P1-7）──
    private TrendMonitor trendMonitor = new TrendMonitor();

    // ── 因果推理引擎（贝叶斯基准率 + 因果因子配置化）──
    private CausalEngine causalEngine = new CausalEngine();

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
        /** 产品类型因子（护肤彩妆，不含食品零食） */
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
