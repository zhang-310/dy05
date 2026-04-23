package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.List;
import java.util.Map;

/**
 * 增长路径规划服务（Phase2）
 * 根据当前状态与目标生成分阶段策略
 */
public interface GrowthPathService {

    /**
     * 生成增长路径
     *
     * @param accountId     账号 ID
     * @param userId        用户 ID
     * @param currentState  当前粉丝数、内容数、互动率等
     * @param targetFans    目标粉丝数（如 10000, 50000, 100000）
     * @return 分阶段路径
     */
    GrowthPathResult generate(Long accountId, Long userId, Map<String, Object> currentState, long targetFans);

    boolean isAvailable();

    record GrowthPathResult(
            List<PhasePlan> phases,
            String summary,
            List<String> criticalSuccessFactors
    ) {}

    record PhasePlan(
            int phaseOrder,
            String phaseName,
            long targetFans,
            List<String> strategies,
            List<String> keyMetrics,
            String estimatedDuration
    ) {}
}
