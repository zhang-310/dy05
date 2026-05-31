package cn.gaifan.douyinOperations.contract.credit;

/**
 * 积分冻结超时受控释放请求。
 *
 * <p>该请求只承接已经长时间 RESERVED 的冻结单。调用方必须提供操作者、幂等键和原因，
 * 避免后台任务或人工操作静默释放额度。</p>
 */
public record CreditReservationTimeoutReleaseRequest(
        String tenantId,
        String operatorUserId,
        String idempotencyKey,
        String traceId,
        String reason
) {
}
