package cn.gaifan.douyinOperations.contract.credit;

/**
 * 积分冻结超时受控释放结果。
 *
 * <p>结果同时返回执行台账和标准冻结单结果，让调用方可以核对 RELEASE 流水和余额变化。</p>
 */
public record CreditReservationTimeoutReleaseResult(
        CreditReservationTimeoutReleaseExecutionSummary execution,
        CreditReservationResult reservationResult,
        boolean replayed,
        String message
) {
}
