package cn.gaifan.douyinOperations.common.service.impl;

import cn.gaifan.douyinOperations.common.config.BusinessParamConfig;
import cn.gaifan.douyinOperations.common.service.EmotionCurveService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 公共情绪曲线服务实现：从 BusinessParamConfig 读取模板，解析曲线，生成分段与 BGM 建议。
 */
@Service
public class EmotionCurveServiceImpl implements EmotionCurveService {

    @Resource
    private BusinessParamConfig businessParamConfig;

    private static final int BGM_VOL_MIN = 60;
    private static final int BGM_VOL_MAX = 90;

    @Override
    public List<EmotionPoint> parseCurve(String curveExpression) {
        if (curveExpression == null || curveExpression.isBlank()) return List.of();
        String[] parts = curveExpression.split("[→\\->]+");
        List<EmotionPoint> points = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            try {
                int value = Integer.parseInt(parts[i].trim());
                double timeRatio = parts.length > 1 ? (double) i / (parts.length - 1) : 0.5;
                String intensity = classifyIntensity(value);
                int bgmVolume = mapToBgmVolume(value);
                String speed = mapToSpeed(value);
                points.add(new EmotionPoint(timeRatio, value, intensity, bgmVolume, speed));
            } catch (NumberFormatException ignored) {
                // skip malformed values
            }
        }
        return points;
    }

    @Override
    public List<EmotionSegment> generateSegments(int durationSeconds, List<EmotionPoint> curve) {
        if (curve == null || curve.isEmpty() || durationSeconds <= 0) return List.of();
        List<EmotionSegment> segments = new ArrayList<>();
        for (int i = 0; i < curve.size(); i++) {
            EmotionPoint curr = curve.get(i);
            int startSec = (int) (curr.timeRatio() * durationSeconds);
            int endSec = i + 1 < curve.size()
                    ? (int) (curve.get(i + 1).timeRatio() * durationSeconds)
                    : durationSeconds;
            if (startSec < endSec) {
                segments.add(new EmotionSegment(startSec, endSec, curr.emotionValue(), curr.intensityLabel()));
            }
        }
        return segments;
    }

    @Override
    public List<EmotionCurveTemplate> recommendTemplates(String contentType) {
        Map<String, String> templates = getTemplates();
        List<EmotionCurveTemplate> result = new ArrayList<>();
        Map<String, String> descs = Map.of(
                "hook_climax", "开场强→低谷→高潮→收尾",
                "slow_build", "渐进式升温",
                "rollercoaster", "过山车（爆款常用）",
                "suspense", "悬念式（结尾爆发）",
                "emotional_wave", "情感波浪（种草常用）"
        );
        Map<String, String> names = Map.of(
                "hook_climax", "hook高潮型",
                "slow_build", "渐进型",
                "rollercoaster", "过山车型",
                "suspense", "悬念型",
                "emotional_wave", "情感波浪型"
        );
        for (Map.Entry<String, String> e : templates.entrySet()) {
            result.add(new EmotionCurveTemplate(
                    e.getKey(),
                    names.getOrDefault(e.getKey(), e.getKey()),
                    e.getValue(),
                    descs.getOrDefault(e.getKey(), "")
            ));
        }
        return result;
    }

    @Override
    public BgmRhythmSuggestion suggestBgmRhythm(List<EmotionPoint> curve) {
        if (curve == null || curve.isEmpty()) {
            return new BgmRhythmSuggestion("80-100", "中等节奏", "BGM 音量 70%");
        }
        double avg = curve.stream().mapToInt(EmotionPoint::emotionValue).average().orElse(70);
        double std = stdDev(curve.stream().mapToInt(EmotionPoint::emotionValue).mapToDouble(x -> x).toArray());
        int first = curve.get(0).emotionValue();
        int last = curve.get(curve.size() - 1).emotionValue();

        String bpmRange;
        String rhythmDesc;
        if (avg > 80) {
            bpmRange = "120-140";
            rhythmDesc = "高能量，节奏偏快";
        } else if (avg < 50) {
            bpmRange = "60-80";
            rhythmDesc = "低能量，舒缓节奏";
        } else {
            bpmRange = "80-100";
            rhythmDesc = "中等节奏";
        }
        if (std > 20) {
            rhythmDesc += "，需随情绪起伏变化";
        } else if (std < 10) {
            rhythmDesc += "，保持稳定";
        }
        if (last > first + 10) {
            rhythmDesc += "，渐进式增强";
        } else if (last < first - 10) {
            rhythmDesc += "，回落收尾";
        }
        return new BgmRhythmSuggestion(bpmRange, rhythmDesc, "BGM 音量随情绪值 60%-90% 调节");
    }

    @Override
    public EmotionPoint getEmotionAt(List<EmotionPoint> curve, double progress) {
        if (curve == null || curve.isEmpty()) {
            return new EmotionPoint(progress, 100, "中等", 70, "正常");
        }
        if (curve.size() == 1) return curve.get(0);
        progress = Math.max(0, Math.min(1, progress));

        EmotionPoint prev = curve.get(0);
        for (int i = 1; i < curve.size(); i++) {
            EmotionPoint curr = curve.get(i);
            if (progress <= curr.timeRatio()) {
                double segmentRatio = curr.timeRatio() - prev.timeRatio();
                double localRatio = segmentRatio > 0 ? (progress - prev.timeRatio()) / segmentRatio : 0;
                int interpolated = (int) (prev.emotionValue() + (curr.emotionValue() - prev.emotionValue()) * localRatio);
                return new EmotionPoint(progress, interpolated,
                        classifyIntensity(interpolated),
                        mapToBgmVolume(interpolated),
                        mapToSpeed(interpolated));
            }
            prev = curr;
        }
        return curve.get(curve.size() - 1);
    }

    private Map<String, String> getTemplates() {
        if (businessParamConfig != null && businessParamConfig.getEmotionCurveTemplates() != null
                && !businessParamConfig.getEmotionCurveTemplates().isEmpty()) {
            return businessParamConfig.getEmotionCurveTemplates();
        }
        return Map.of(
                "hook_climax", "90→60→40→80→100→70",
                "slow_build", "40→50→60→75→90→100",
                "rollercoaster", "80→40→90→30→100→60",
                "suspense", "60→70→50→40→30→100",
                "emotional_wave", "70→90→50→85→40→95"
        );
    }

    private String classifyIntensity(int value) {
        if (value >= 120) return "爆发";
        if (value >= 100) return "高能";
        if (value >= 80) return "中等";
        if (value >= 60) return "蓄力";
        return "低沉";
    }

    private int mapToBgmVolume(int emotionValue) {
        return (int) (BGM_VOL_MIN + (BGM_VOL_MAX - BGM_VOL_MIN) * Math.min(1.0, emotionValue / 130.0));
    }

    private String mapToSpeed(int emotionValue) {
        if (emotionValue >= 120) return "快速激昂";
        if (emotionValue >= 100) return "偏快有力";
        if (emotionValue >= 80) return "正常";
        if (emotionValue >= 60) return "稍慢舒缓";
        return "缓慢沉稳";
    }

    private static double stdDev(double[] values) {
        if (values == null || values.length == 0) return 0;
        double mean = 0;
        for (double v : values) mean += v;
        mean /= values.length;
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.length);
    }
}
