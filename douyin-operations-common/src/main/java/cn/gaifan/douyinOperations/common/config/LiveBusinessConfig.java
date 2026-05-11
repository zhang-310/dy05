package cn.gaifan.douyinOperations.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 直播业务参数配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.business.live")
public class LiveBusinessConfig {

    // ── 30分钟标准段时间分配（秒）──
    private LiveSegment liveSegment = new LiveSegment();

    // ── 留人策略频率（分钟）──
    private RetentionFrequency retention = new RetentionFrequency();

    // ── 价值塑造公式时间分配（秒）──
    private ValueFormula valueFormula = new ValueFormula();

    /** 直播讲解库存提示 */
    private LiveInventory liveInventory = new LiveInventory();

    /** 实时话术建议阈值 */
    private RealtimeSuggestion realtimeSuggestion = new RealtimeSuggestion();

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
    public static class LiveInventory {
        /** 是否在 LLM 生成话术的 user prompt 中追加库存提醒 */
        private boolean promptHintEnabled = true;
        /** 是否在实时辅助面板建议中提示低库存 */
        private boolean realtimeSuggestionEnabled = true;
        /** 库存 ≤ 此值视为偏紧 */
        private long lowStockThreshold = 50;
        /** 库存 ≤ 此值视为极低 */
        private long criticalStockThreshold = 10;
    }

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
}
