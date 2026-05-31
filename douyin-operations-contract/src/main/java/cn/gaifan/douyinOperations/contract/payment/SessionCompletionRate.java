package cn.gaifan.douyinOperations.contract.payment;

/**
 * 场次完播率
 */
public record SessionCompletionRate(
        Long sessionId,
        double avgWatchDuration,
        double fullWatchRatio,
        int viewerCount,
        String dataSource
) {
    public static SessionCompletionRate estimated(Long sessionId) {
        return new SessionCompletionRate(sessionId, 0, 0.85, 0, "estimated");
    }
}
