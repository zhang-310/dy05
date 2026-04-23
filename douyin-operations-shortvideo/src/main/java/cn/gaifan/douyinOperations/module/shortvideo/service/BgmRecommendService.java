package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.service.EmotionCurveService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.BgmRecommendVO;

import java.util.List;

/**
 * BGM 智能推荐服务（Phase 2.7）
 */
public interface BgmRecommendService {

    List<BgmRecommendVO> recommendByEmotionCurve(List<EmotionCurveService.EmotionPoint> curve);

    List<BgmRecommendVO> recommendByScript(String scriptContent, String contentType);
}
