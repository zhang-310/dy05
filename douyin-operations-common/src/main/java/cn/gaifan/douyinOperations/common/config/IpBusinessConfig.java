package cn.gaifan.douyinOperations.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * IP运营业务参数配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.business.ip")
public class IpBusinessConfig {

    // ── IP成长阶段粉丝数阈值 ──
    private IpGrowth ipGrowth = new IpGrowth();

    // ── 现象级/顶级 IP 运营指标阈值 ──
    private IpMetrics ipMetrics = new IpMetrics();

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
}
