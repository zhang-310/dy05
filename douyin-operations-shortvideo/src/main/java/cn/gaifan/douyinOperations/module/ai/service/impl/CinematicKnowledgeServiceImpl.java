package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.domain.CameraType;
import cn.gaifan.douyinOperations.module.ai.service.CinematicKnowledgeService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvCinematicPreset;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvGenerationLog;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvSceneCameraMapping;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvCinematicPresetRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvGenerationLogRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvSceneCameraMappingRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 运镜/脚本知识库服务 (Phase 5)
 */
@Service
public class CinematicKnowledgeServiceImpl implements CinematicKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(CinematicKnowledgeServiceImpl.class);

    @Resource
    private SvSceneCameraMappingRepository sceneCameraMappingRepository;
    @Resource
    private SvCinematicPresetRepository cinematicPresetRepository;
    @Resource
    private SvGenerationLogRepository generationLogRepository;

    @Override
    public List<CameraRecommendation> recommendCamera(String sceneDescription) {
        if (!StringUtils.hasText(sceneDescription)) {
            return List.of(new CameraRecommendation(CameraType.ZOOM_IN, 0.5, "默认推荐", "kling", 0));
        }
        String desc = sceneDescription.toLowerCase();
        List<SvSceneCameraMapping> all = sceneCameraMappingRepository.findAllByOrderByConfidenceDesc();
        List<CameraRecommendation> result = new ArrayList<>();
        for (SvSceneCameraMapping m : all) {
            if (desc.contains(m.getSceneKeyword().toLowerCase())) {
                CameraType ct = CameraType.fromCode(m.getRecommendedCamera());
                double conf = m.getConfidence() != null ? m.getConfidence().doubleValue() : 0.5;
                String bestModel = "kling";
                double avgScore = 0;
                List<SvCinematicPreset> presets = cinematicPresetRepository.findByCameraTypeAndDeletedOrderBySuccessRateDesc(ct.getCode(), 0);
                if (!presets.isEmpty()) {
                    SvCinematicPreset p = presets.get(0);
                    bestModel = StringUtils.hasText(p.getBestModel()) ? p.getBestModel() : bestModel;
                    avgScore = p.getAvgQualityScore() != null ? p.getAvgQualityScore().doubleValue() : 0;
                }
                result.add(new CameraRecommendation(ct, conf, "场景关键词匹配: " + m.getSceneKeyword(), bestModel, avgScore));
            }
        }
        result.sort(Comparator.comparingDouble(CameraRecommendation::confidence).reversed());
        if (result.isEmpty()) {
            result.add(new CameraRecommendation(CameraType.ZOOM_IN, 0.5, "默认推荐", "kling", 0));
        }
        return result;
    }

    @Override
    public String recommendCameraCode(String sceneDescription) {
        List<CameraRecommendation> list = recommendCamera(sceneDescription);
        return list.isEmpty() ? "zoom-in" : list.get(0).cameraType().getCode();
    }

    @Override
    public void logGeneration(Long projectId, Long shotId, String cameraType,
                              String provider, boolean success, Double qualityScore,
                              long generationTimeMs, String prompt, String errorMessage) {
        try {
            SvGenerationLog logEntity = new SvGenerationLog();
            logEntity.setProjectId(projectId);
            logEntity.setShotId(shotId);
            logEntity.setCameraType(cameraType);
            logEntity.setAiProvider(provider);
            logEntity.setSuccess(success);
            logEntity.setQualityScore(qualityScore != null ? BigDecimal.valueOf(qualityScore) : null);
            logEntity.setGenerationTimeMs(generationTimeMs);
            logEntity.setPrompt(prompt != null && prompt.length() > 2000 ? prompt.substring(0, 2000) : prompt);
            logEntity.setErrorMessage(errorMessage != null && errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage);
            logEntity.setCreateTime(new Timestamp(System.currentTimeMillis()));
            generationLogRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("logGeneration 写入失败: {}", e.getMessage());
        }
    }
}
