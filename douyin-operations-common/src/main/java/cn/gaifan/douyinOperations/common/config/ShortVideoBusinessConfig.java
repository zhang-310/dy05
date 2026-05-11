package cn.gaifan.douyinOperations.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 短视频业务参数配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.business.shortvideo")
public class ShortVideoBusinessConfig {

    // ── 爆款识别阈值 ──
    private ViralThreshold viral = new ViralThreshold();

    // ── 热点时间窗口 ──
    private HotspotWindow hotspot = new HotspotWindow();

    // ── 短视频分镜推荐规则 ──
    private ShotCountRule shotCountRule = new ShotCountRule();

    // ── 情绪曲线模板 ──
    private EmotionCurve emotionCurve = new EmotionCurve();

    /** 短视频情绪曲线模板：code -> curveExpression */
    private Map<String, String> emotionCurveTemplates = Map.of(
            "hook_climax", "90→60→40→80→100→70",
            "slow_build", "40→50→60→75→90→100",
            "rollercoaster", "80→40→90→30→100→60",
            "suspense", "60→70→50→40→30→100",
            "emotional_wave", "70→90→50→85→40→95"
    );

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
}
