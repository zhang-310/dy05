package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.Map;

public interface ContentEffectPredictor {
    Map<String, Object> predict(Long userId, String scriptContent, String title, String publishTime);

    /** H-4：按当前配置的 calibration-factor 试算 confidence（不写库、不调 LLM） */
    Map<String, Object> previewConfidenceCalibration(Double confidence);
}
