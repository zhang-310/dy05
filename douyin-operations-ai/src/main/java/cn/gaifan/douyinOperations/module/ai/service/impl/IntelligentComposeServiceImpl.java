package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.IntelligentComposeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能合成服务实现 (Phase 8)
 * 基于素材元数据的规则降级：未接入 BPM 检测时不声称真实音频节拍分析。
 */
@Service
public class IntelligentComposeServiceImpl implements IntelligentComposeService {

    @Override
    public List<BeatSegment> computeBeatSync(String bgmUrl, int clipCount) {
        if (clipCount <= 0) return List.of();
        List<BeatSegment> segments = new ArrayList<>();
        double avgDuration = 5.0; // 默认每段 5 秒
        for (int i = 0; i < clipCount; i++) {
            double start = i * avgDuration;
            double end = (i + 1) * avgDuration;
            double intensity = 0.5 + 0.3 * Math.sin(i * 0.5);
            segments.add(new BeatSegment(start, end, Math.max(0, Math.min(1, intensity))));
        }
        return segments;
    }

    @Override
    public String suggestTransition(String prevClip, String nextClip) {
        if (prevClip == null || nextClip == null) return "fade";
        String p = prevClip.toLowerCase();
        String n = nextClip.toLowerCase();
        if (p.contains("紧张") || p.contains("打斗") || n.contains("平静")) return "dissolve";
        if (p.contains("回忆") || n.contains("现实")) return "fade";
        return "cut";
    }

    @Override
    public List<Double> suggestTensionCurve(int totalDuration, int clipCount) {
        if (clipCount <= 0 || totalDuration <= 0) return List.of();
        List<Double> ratios = new ArrayList<>();
        for (int i = 0; i < clipCount; i++) {
            double t = (double) i / (clipCount - 1);
            double r = 0.8 + 0.4 * Math.sin(t * Math.PI);
            ratios.add(Math.max(0.5, Math.min(1.5, r)));
        }
        double sum = ratios.stream().mapToDouble(Double::doubleValue).sum();
        for (int i = 0; i < ratios.size(); i++) {
            ratios.set(i, ratios.get(i) / sum);
        }
        return ratios;
    }
}
