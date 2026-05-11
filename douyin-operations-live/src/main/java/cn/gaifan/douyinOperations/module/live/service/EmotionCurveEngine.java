package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.config.ShortVideoBusinessConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 情绪曲线解析引擎。
 * 将 "100→80→60→120→100→90→130→100" 解析为时间轴映射的情绪节点列表。
 * 支持情绪值映射到 BGM 音量、语速、语气强度。
 */
@Component
public class EmotionCurveEngine {

    @Resource
    private ShortVideoBusinessConfig shortVideoConfig;

    public record EmotionPoint(
            double timeRatio,
            int emotionValue,
            String intensityLabel,
            int bgmVolumePercent,
            String speedHint
    ) {}

    /**
     * 按 IP 类型解析情绪曲线为时间轴节点列表。
     * @param ipType "phenomenal" 或 "top"
     * @return 按时间比例排列的情绪节点
     */
    public List<EmotionPoint> parseCurve(String ipType) {
        ShortVideoBusinessConfig.EmotionCurve ec = shortVideoConfig.getEmotionCurve();
        String curveStr = "phenomenal".equals(ipType) ? ec.getPhenomenalCurve() : ec.getTopCurve();
        return parseCurveString(curveStr);
    }

    /**
     * 解析任意情绪曲线字符串。
     * 格式："100→80→60→120→100→90→130→100"
     */
    public List<EmotionPoint> parseCurveString(String curveStr) {
        if (curveStr == null || curveStr.isBlank()) return List.of();

        String[] parts = curveStr.split("[→\\->]+");
        List<EmotionPoint> points = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            try {
                int value = Integer.parseInt(parts[i].trim());
                double timeRatio = parts.length > 1 ? (double) i / (parts.length - 1) : 0.5;
                String intensity = classifyIntensity(value);
                int bgmVolume = mapToBgmVolume(value);
                String speed = mapToSpeed(value);
                points.add(new EmotionPoint(timeRatio, value, intensity, bgmVolume, speed));
            } catch (NumberFormatException e) {
                // skip malformed values
            }
        }
        return points;
    }

    /**
     * 获取某个时间进度（0.0~1.0）对应的插值情绪点。
     */
    public EmotionPoint getEmotionAt(String ipType, double progress) {
        List<EmotionPoint> points = parseCurve(ipType);
        if (points.isEmpty()) return new EmotionPoint(progress, 100, "中等", 70, "正常");
        if (points.size() == 1) return points.get(0);

        progress = Math.max(0, Math.min(1, progress));

        EmotionPoint prev = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            EmotionPoint curr = points.get(i);
            if (progress <= curr.timeRatio()) {
                double segmentRatio = (curr.timeRatio() - prev.timeRatio());
                double localRatio = segmentRatio > 0 ? (progress - prev.timeRatio()) / segmentRatio : 0;
                int interpolatedValue = (int) (prev.emotionValue() + (curr.emotionValue() - prev.emotionValue()) * localRatio);
                return new EmotionPoint(progress, interpolatedValue,
                        classifyIntensity(interpolatedValue),
                        mapToBgmVolume(interpolatedValue),
                        mapToSpeed(interpolatedValue));
            }
            prev = curr;
        }
        return points.get(points.size() - 1);
    }

    /**
     * 生成情绪曲线描述，适合注入 prompt。
     */
    public String buildEmotionPromptSegment(String ipType, int slotIndex, int totalSlots) {
        if (totalSlots <= 0) return "";
        double progress = totalSlots > 1 ? (double) slotIndex / (totalSlots - 1) : 0.5;
        EmotionPoint point = getEmotionAt(ipType, progress);
        return String.format(
                "【当前情绪节点】进度%.0f%%, 情绪强度=%d(%s), BGM音量=%d%%, 语速=%s",
                progress * 100, point.emotionValue(), point.intensityLabel(),
                point.bgmVolumePercent(), point.speedHint());
    }

    private String classifyIntensity(int value) {
        if (value >= 120) return "爆发";
        if (value >= 100) return "高能";
        if (value >= 80) return "中等";
        if (value >= 60) return "蓄力";
        return "低沉";
    }

    private int mapToBgmVolume(int emotionValue) {
        ShortVideoBusinessConfig.BgmVolume bgm = shortVideoConfig.getBgmVolume();
        int minVol = bgm.getFrontVolumeMin();
        int maxVol = bgm.getBackVolumeMax();
        return (int) (minVol + (maxVol - minVol) * Math.min(1.0, emotionValue / 130.0));
    }

    private String mapToSpeed(int emotionValue) {
        if (emotionValue >= 120) return "快速激昂";
        if (emotionValue >= 100) return "偏快有力";
        if (emotionValue >= 80) return "正常";
        if (emotionValue >= 60) return "稍慢舒缓";
        return "缓慢沉稳";
    }
}
