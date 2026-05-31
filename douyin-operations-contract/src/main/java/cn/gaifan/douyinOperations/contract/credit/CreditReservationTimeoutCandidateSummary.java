package cn.gaifan.douyinOperations.contract.credit;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 积分冻结超时释放候选摘要。
 *
 * <p>候选只表示冻结单长时间停留在 RESERVED 状态，需要运营或系统任务复核；
 * 它不会自动释放积分，真实释放必须继续调用受控释放执行接口。</p>
 */
public record CreditReservationTimeoutCandidateSummary(
        String reservationId,
        String tenantId,
        String userId,
        String agentId,
        String productCode,
        String featureCode,
        String channel,
        BigDecimal reservedCredits,
        CreditReservationStatus status,
        String traceId,
        String businessKey,
        String reason,
        long ageMinutes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
