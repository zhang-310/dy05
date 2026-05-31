package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * LF-05：爆款 × 人设融合二创、热点三要素融合
 */
public interface PersonaViralFusionService {

    List<Map<String, Object>> matchPersonas(Long viralVideoId, Long userId);

    Map<String, Object> generatePersonaFusedScript(Long viralVideoId, Long personaId, String remakeType, Long userId);

    default Map<String, Object> generatePersonaFusedScript(
            Long viralVideoId,
            Long personaId,
            String remakeType,
            PersonaFusionOptions options,
            Long userId) {
        return generatePersonaFusedScript(viralVideoId, personaId, remakeType, userId);
    }

    Map<String, Object> generateHotspotFusedScript(Long hotTopicId, Long personaId, Long productId, Long userId);

    record PersonaFusionOptions(
            Long productId,
            String topic,
            Integer durationSeconds,
            Integer count
    ) {}
}
