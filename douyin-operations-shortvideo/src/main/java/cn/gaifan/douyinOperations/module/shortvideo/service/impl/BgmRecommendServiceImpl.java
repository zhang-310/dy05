package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.service.EmotionCurveService;
import cn.gaifan.douyinOperations.module.shortvideo.service.BgmRecommendService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.BgmRecommendVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * BGM 智能推荐实现（Phase 2.7）
 */
@Service
public class BgmRecommendServiceImpl implements BgmRecommendService {

    @Override
    public List<BgmRecommendVO> recommendByEmotionCurve(List<EmotionCurveService.EmotionPoint> curve) {
        List<BgmRecommendVO> list = new ArrayList<>();
        if (curve == null || curve.isEmpty()) {
            list.add(createBgmVO("upbeat_pop", "80-100", "温暖", 70, "默认推荐中等节奏"));
            return list;
        }
        double avg = curve.stream().mapToInt(EmotionCurveService.EmotionPoint::emotionValue).average().orElse(70);
        double std = stdDev(curve.stream().mapToInt(EmotionCurveService.EmotionPoint::emotionValue).mapToDouble(x -> x).toArray());
        int first = curve.get(0).emotionValue();
        int last = curve.get(curve.size() - 1).emotionValue();

        if (avg > 80) {
            list.add(createBgmVO("upbeat_pop", "120-140", "激昂", 80, "情绪曲线高能量，推荐节奏偏快"));
            list.add(createBgmVO("energetic_folk", "100-120", "欢快", 75, "高能量场景适合活力 BGM"));
        } else if (avg < 50) {
            list.add(createBgmVO("emotional_strings", "60-80", "治愈", 85, "情绪曲线偏低，推荐舒缓治愈"));
            list.add(createBgmVO("chill_lofi", "70-90", "温暖", 80, "低能量场景适合放松节奏"));
        } else {
            list.add(createBgmVO("luxury_piano", "80-100", "温暖", 75, "中等情绪曲线，推荐平衡型 BGM"));
        }
        if (std > 20) {
            list.add(createBgmVO("emotional_strings", "80-120", "悬疑", 70, "情绪波动大，需节奏变化的 BGM"));
        }
        if (last > first + 10) {
            list.add(createBgmVO("energetic_folk", "90-110", "激昂", 72, "渐进式升温，推荐逐渐增强的 BGM"));
        }
        return list.stream().limit(3).toList();
    }

    @Override
    public List<BgmRecommendVO> recommendByScript(String scriptContent, String contentType) {
        List<BgmRecommendVO> list = new ArrayList<>();
        if (scriptContent != null && scriptContent.contains("种草")) {
            list.add(createBgmVO("emotional_strings", "70-90", "温暖", 78, "种草内容适合温暖治愈 BGM"));
        }
        if (scriptContent != null && (scriptContent.contains("秒杀") || scriptContent.contains("促销"))) {
            list.add(createBgmVO("upbeat_pop", "120-140", "激昂", 82, "促销场景适合高能量 BGM"));
        }
        if (list.isEmpty()) {
            list.add(createBgmVO("luxury_piano", "80-100", "温暖", 70, "根据内容类型推荐平衡型 BGM"));
        }
        return list.stream().limit(3).toList();
    }

    private BgmRecommendVO createBgmVO(String style, String bpmRange, String mood, int matchScore, String reason) {
        BgmRecommendVO vo = new BgmRecommendVO();
        vo.setStyle(style);
        vo.setBpmRange(bpmRange);
        vo.setMood(mood);
        vo.setMatchScore(matchScore);
        vo.setReason(reason);
        return vo;
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
