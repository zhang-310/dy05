package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 积分冻结超时释放执行台账摘要。
 *
 * <p>该台账记录超时释放的幂等键、审计事件和 RELEASE 流水，便于排查长任务、
 * 供应商回调和额度余额之间的关系。</p>
 */
public record CreditReservationTimeoutReleaseExecutionSummary(
        String releaseId,
        String reservationId,
        String tenantId,
        CreditReservationStatus oldStatus,
        CreditReservationStatus newStatus,
        BigDecimal reservedCredits,
        String idempotencyKey,
        String releaseEntryId,
        String auditId,
        String traceId,
        String executedByUserId,
        OffsetDateTime executedAt
) {
}
