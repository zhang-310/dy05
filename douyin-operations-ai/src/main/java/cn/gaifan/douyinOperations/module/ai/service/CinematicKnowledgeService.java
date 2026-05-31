package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.domain.CameraType;

import java.util.List;

/**
 * 运镜/脚本知识库（Phase 5）；实现类依赖 shortvideo 仓储时可置于 app 域。
 */
public interface CinematicKnowledgeService {

    List<CameraRecommendation> recommendCamera(String sceneDescription);

    String recommendCameraCode(String sceneDescription);

    void logGeneration(Long projectId, Long shotId, String cameraType,
                       String provider, boolean success, Double qualityScore,
                       long generationTimeMs, String prompt, String errorMessage);

    record CameraRecommendation(
            CameraType cameraType,
            double confidence,
            String reason,
            String bestModel,
            double avgQualityScore
    ) {}
}
