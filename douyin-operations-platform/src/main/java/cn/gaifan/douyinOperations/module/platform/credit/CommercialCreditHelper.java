package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import cn.gaifan.douyinOperations.contract.credit.CreditQuoteRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditQuoteResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationActionRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationStatus;
import cn.gaifan.douyinOperations.contract.credit.CreditReserveRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 商业化积分扣费辅助：{@code app.credit.enforce=false} 时全放行，开启后走 {@link CreditLedgerService}。
 */
@Service
public class CommercialCreditHelper {

    private final CreditLedgerService creditLedgerService;

    @Value("${app.credit.enforce:false}")
    private boolean creditEnforce;

    public CommercialCreditHelper(CreditLedgerService creditLedgerService) {
        this.creditLedgerService = creditLedgerService;
    }

    public boolean isEnforced() {
        return creditEnforce;
    }

    public CreditQuoteResult quote(CreditQuoteRequest request) {
        if (!creditEnforce) {
            return bypassQuote(request);
        }
        return creditLedgerService.quote(request);
    }

    public CreditConsumeResult consume(CreditConsumeRequest request) {
        if (!creditEnforce) {
            return bypassConsume(request);
        }
        CreditConsumeResult result = creditLedgerService.consume(request);
        if (!result.allowed()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS, result.message() != null ? result.message() : "积分不足");
        }
        return result;
    }

    public CreditReservationResult reserve(CreditReserveRequest request) {
        if (!creditEnforce) {
            return bypassReserve(request);
        }
        CreditReservationResult result = creditLedgerService.reserve(request);
        if (!result.allowed()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS, result.message() != null ? result.message() : "积分不足");
        }
        return result;
    }

    public CreditReservationResult commit(String reservationId, CreditReservationActionRequest request) {
        if (!creditEnforce) {
            return bypassCommit(reservationId, request);
        }
        return creditLedgerService.commit(reservationId, request);
    }

    public CreditReservationResult release(String reservationId, CreditReservationActionRequest request) {
        if (!creditEnforce) {
            return bypassRelease(reservationId);
        }
        return creditLedgerService.release(reservationId, request);
    }

    private static CreditQuoteResult bypassQuote(CreditQuoteRequest request) {
        return new CreditQuoteResult(
                "quote-bypass",
                request.tenantId(),
                request.productCode(),
                request.featureCode(),
                request.channel(),
                normalizeAmount(request.requestedAmount()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("99999"),
                true,
                "credit.enforce=false",
                request.traceId()
        );
    }

    private static CreditConsumeResult bypassConsume(CreditConsumeRequest request) {
        CreditQuoteResult quote = bypassQuote(new CreditQuoteRequest(
                request.tenantId(),
                request.userId(),
                request.agentId(),
                request.productCode(),
                request.featureCode(),
                request.channel(),
                request.requestedAmount(),
                request.pricingTier(),
                request.traceId()
        ));
        return new CreditConsumeResult(
                "credit_txn_bypass",
                true,
                "ALLOW",
                "credit.enforce=false",
                quote,
                BigDecimal.ZERO,
                quote.availableBefore(),
                null,
                null
        );
    }

    private static CreditReservationResult bypassReserve(CreditReserveRequest request) {
        CreditQuoteResult quote = bypassQuote(new CreditQuoteRequest(
                request.tenantId(),
                request.userId(),
                request.agentId(),
                request.productCode(),
                request.featureCode(),
                request.channel(),
                request.requestedAmount(),
                request.pricingTier(),
                request.traceId()
        ));
        return new CreditReservationResult(
                "reservation_bypass",
                CreditReservationStatus.RESERVED,
                true,
                "ALLOW",
                "credit.enforce=false",
                quote,
                BigDecimal.ZERO,
                quote.availableBefore(),
                null,
                null,
                null
        );
    }

    private static CreditReservationResult bypassCommit(String reservationId, CreditReservationActionRequest request) {
        return new CreditReservationResult(
                reservationId,
                CreditReservationStatus.COMMITTED,
                true,
                "COMMITTED",
                "credit.enforce=false",
                null,
                BigDecimal.ZERO,
                new BigDecimal("99999"),
                null,
                null,
                null
        );
    }

    private static CreditReservationResult bypassRelease(String reservationId) {
        return new CreditReservationResult(
                reservationId,
                CreditReservationStatus.RELEASED,
                true,
                "RELEASED",
                "credit.enforce=false",
                null,
                BigDecimal.ZERO,
                new BigDecimal("99999"),
                null,
                null,
                null
        );
    }

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null || amount.signum() <= 0 ? BigDecimal.ONE : amount;
    }
}
