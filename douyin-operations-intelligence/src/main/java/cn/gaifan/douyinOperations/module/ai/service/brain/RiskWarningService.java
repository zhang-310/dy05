package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.List;

/**
 * 风险预警服务（Phase2）
 * 能力：违规预警、敏感词检测、合规建议
 */
public interface RiskWarningService {

    /**
     * 对内容进行风险检测
     *
     * @param content 待检内容
     * @param userId  用户 ID
     * @return 风险项列表
     */
    List<RiskItem> warn(String content, Long userId);

    /**
     * 批量检测（如脚本、话术列表）
     */
    List<RiskItem> warnBatch(List<String> contents, Long userId);

    /**
     * 获取违规预警准确率等统计（供监控）
     */
    RiskStats getStats();

    boolean isAvailable();

    record RiskItem(
            int level,           // 1=低 2=中 3=高
            String type,         // violation/sensitive/compliance
            String message,
            int startOffset,
            int endOffset,
            String suggestion
    ) {}

    record RiskStats(long totalChecks, long violationCount, double accuracyEstimate) {}
}
