package cn.gaifan.douyinOperations.module.shortvideo.service;

/**
 * 爆款二创闭环状态机：0→1→2→3→4→5
 */
public interface ViralRemakeService {

    void recommendRemake(Long viralVideoId, Long userId);

    int batchRecommend(Long userId, double scoreThreshold, int limit);

    void confirmRemake(Long viralVideoId, String selectedRemakeType, Long personaId, Long userId);

    /**
     * @param scriptMode {@code sop} 五阶段复刻方案（默认）；{@code persona_fusion} 人设融合结构化脚本
     */
    Long generateRemakeScript(Long viralVideoId, Long userId, String scriptMode);

    default Long generateRemakeScript(Long viralVideoId, Long userId) {
        return generateRemakeScript(viralVideoId, userId, "sop");
    }

    Long assignToShootingTask(Long viralVideoId, Long photographerId, String shootDate, Long userId);

    void markCompleted(Long viralVideoId, Long userId);
}
