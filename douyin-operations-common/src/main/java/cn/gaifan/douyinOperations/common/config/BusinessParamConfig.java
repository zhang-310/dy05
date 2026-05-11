package cn.gaifan.douyinOperations.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;

/**
 * 业务参数配置聚合入口（向后兼容）
 *
 * <p>已按业务领域拆分为：
 * <ul>
 *   <li>{@link ShortVideoBusinessConfig} - 短视频配置</li>
 *   <li>{@link LiveBusinessConfig} - 直播配置</li>
 *   <li>{@link IpBusinessConfig} - IP运营配置</li>
 *   <li>{@link AttributionConfig} - 归因分析配置</li>
 * </ul>
 *
 * <p>建议直接注入具体配置类，而非使用此聚合类。
 *
 * @deprecated 使用具体配置类替代，将在 v3.0 移除
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.business")
@Deprecated
public class BusinessParamConfig {

    @Resource
    private ShortVideoBusinessConfig shortVideoConfig;

    @Resource
    private LiveBusinessConfig liveConfig;

    @Resource
    private IpBusinessConfig ipConfig;

    @Resource
    private AttributionConfig attributionConfig;

    // ========== 向后兼容：委托到子配置 ==========

    public ShortVideoBusinessConfig.ViralThreshold getViral() {
        return shortVideoConfig.getViral();
    }

    public ShortVideoBusinessConfig.HotspotWindow getHotspot() {
        return shortVideoConfig.getHotspot();
    }

    public LiveBusinessConfig.LiveSegment getLiveSegment() {
        return liveConfig.getLiveSegment();
    }

    public LiveBusinessConfig.RetentionFrequency getRetention() {
        return liveConfig.getRetention();
    }

    public LiveBusinessConfig.ValueFormula getValueFormula() {
        return liveConfig.getValueFormula();
    }

    public IpBusinessConfig.IpGrowth getIpGrowth() {
        return ipConfig.getIpGrowth();
    }

    public ShortVideoBusinessConfig.RemakeSop getRemakeSop() {
        return shortVideoConfig.getRemakeSop();
    }

    public ShortVideoBusinessConfig.BgmVolume getBgmVolume() {
        return shortVideoConfig.getBgmVolume();
    }

    public IpBusinessConfig.IpMetrics getIpMetrics() {
        return ipConfig.getIpMetrics();
    }

    public ShortVideoBusinessConfig.ShotCountRule getShotCountRule() {
        return shortVideoConfig.getShotCountRule();
    }

    public ShortVideoBusinessConfig.EmotionCurve getEmotionCurve() {
        return shortVideoConfig.getEmotionCurve();
    }

    public LiveBusinessConfig.LiveInventory getLiveInventory() {
        return liveConfig.getLiveInventory();
    }

    public LiveBusinessConfig.RealtimeSuggestion getRealtimeSuggestion() {
        return liveConfig.getRealtimeSuggestion();
    }

    public java.util.Map<String, String> getEmotionCurveTemplates() {
        return shortVideoConfig.getEmotionCurveTemplates();
    }

    public AttributionConfig.Attribution getAttribution() {
        return attributionConfig.getAttribution();
    }

    public AttributionConfig.Effectiveness getEffectiveness() {
        return attributionConfig.getEffectiveness();
    }

    public AttributionConfig.VersionComparison getVersionComparison() {
        return attributionConfig.getVersionComparison();
    }

    public AttributionConfig.AutoImprove getAutoImprove() {
        return attributionConfig.getAutoImprove();
    }

    public AttributionConfig.KnowledgeQuality getKnowledgeQuality() {
        return attributionConfig.getKnowledgeQuality();
    }

    public java.util.Map<String, AttributionConfig.IndustryBenchmark> getIndustryBenchmark() {
        return attributionConfig.getIndustryBenchmark();
    }

    public AttributionConfig.TrendMonitor getTrendMonitor() {
        return attributionConfig.getTrendMonitor();
    }

    public AttributionConfig.CausalEngine getCausalEngine() {
        return attributionConfig.getCausalEngine();
    }
}
