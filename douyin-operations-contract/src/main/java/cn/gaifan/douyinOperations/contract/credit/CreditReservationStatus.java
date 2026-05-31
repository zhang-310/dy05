package cn.gaifan.douyinOperations.contract.credit;

/**
 * 积分冻结单状态。
 *
 * <p>状态只描述一笔冻结单的商业生命周期，不直接代表下游视频、AI 或 MCP 任务状态。
 * 下游任务必须用同一个 traceId 或 businessKey 与本冻结单串联。</p>
 */
public enum CreditReservationStatus {
    /** 已冻结，等待业务任务成功提交或失败释放。 */
    RESERVED,
    /** 业务任务成功，冻结积分已经转为正式消耗。 */
    COMMITTED,
    /** 业务任务失败、取消或未执行，冻结积分已经返还可用余额。 */
    RELEASED
}
