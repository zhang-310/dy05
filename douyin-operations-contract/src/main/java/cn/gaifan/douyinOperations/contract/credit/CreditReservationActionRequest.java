package cn.gaifan.douyinOperations.contract.credit;

/**
 * 积分冻结单动作请求。
 *
 * <p>commit 和 release 都只允许作用于已有冻结单。reason 用于补充本次业务任务成功、
 * 失败、取消或超时的原因，避免审计台账只能看到机械状态。</p>
 */
public record CreditReservationActionRequest(
        String tenantId,
        String userId,
        String agentId,
        String channel,
        String traceId,
        String reason
) {
}
